package net.ackx.tradr

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

import static com.adessa.unbox.LocalDateHelper.fromLocalDateTime
import static com.adessa.unbox.LocalDateHelper.toLocalDateTime

/**
 * This class contains utility methods to read and store {@link Data} from and to a CSV file.
 * It also convert raw input data collected from a REST API and stored as is into a structured format
 * usable by this application.
 *
 * @author @YouriAckx
 */
@Slf4j
class RawData {

    /**
     * Input data file name.
     * </p>
     * This {@code csv} file contains all data for running a simulation. Sample:
     * <pre>
     * #dateTime,open,close,low,high,volumeFrom,volumeTo
     * 2017-03-17T13:34:00,43.01,42.96,42.74,43.06,207.53,8927.73
     * 2017-03-17T13:35:00,42.96,42.74,42.74,42.96,94.26,4036.05
     * </pre>
     */
    public static final CSV_FILE_PATH = 'stock-history.csv'

    /**
     * Input files name pattern.
     * These are JSON files collected from cryptocompare.com, and lazily stored as they were collected.
     */
    private static final INPUT_FILES_PATTERN = /cryptocurrency.*\.json/

    /**
     * Read data from a CSV file and transform them into a map.
     * @param csvPath Path to CSV input file
     * @return Data as a Map. {@code k=dateTime}, {@code v=data}
     */
    Map<LocalDateTime, Data> readFromCsv(String csvPath) {
        Map<LocalDateTime, Data> data = [:]

        def csv = new File(csvPath)
        if (csv.exists()) {
            for (String line : csv) {
                if (line.startsWith('#')) continue
                def parts = line.split(',')
                def dt = toLocalDateTime(parts[0])
                def tradingData = new Data(dateTime: dt,
                        opening: parts[1] as BigDecimal,
                        closing: parts[2] as BigDecimal,
                        low: parts[3] as BigDecimal,
                        high: parts[4] as BigDecimal,
                        volumeFrom: parts[5] as BigDecimal,
                        volumeTo: parts[6] as BigDecimal,
                )
                data[dt] = tradingData
            }
        }

        data
    }

    /**
     * Read data from several JSON files, and transform them into a map.
     *
     * @param existingData Existing data.
     * @param folderPath Input file folder path. Contains JSON files matching {@link #INPUT_FILES_PATTERN}
     *
     * @return
     * Data as a Map. {@code k=dateTime}, {@code v=data}
     * Contains the {@code existingData} + the data contained in the JSON files
     */
    Map<LocalDateTime, Data> readFromJsonFiles(Map<LocalDateTime, Data> existingData, String folderPath = '.') {
        // Loop all raw json files
        def rawFiles = new File(folderPath).listFiles().findAll({it.name ==~ INPUT_FILES_PATTERN})

        // For each file, capture info
        rawFiles.each { rawFile ->
            def json = new JsonSlurper().parse(rawFile)
            if (!json["Response"] as String == "Success") {
                log.error "File $rawFile is not a correct cryptocompare JSON raw file"
            }
            log.info "Processing $rawFile"
            json['Data'].each { d ->
                def dt = LocalDateTime.ofInstant(Instant.ofEpochMilli((d.time as long) * 1000), ZoneId.systemDefault())
                def td = new Data(dateTime: dt, opening: d.open, closing: d.close,
                        low: d.low, high: d.high, volumeFrom: d.volumefrom, volumeTo: d.volumeto)
                existingData[dt] = td
            }
        }
        log.info "We have ${existingData.keySet().size()} entries"

        existingData
    }

    /**
     * Sort the given map based on {@link Data#dateTime}
     * @param data Data: {@code k=dateTime}, {@code v=data}
     * @return Sorted data, as a list
     */
    List<Data> sortDataMap(Map<LocalDateTime, Data> data) {
        data.values().sort { a, b -> a.dateTime <=> b.dateTime }
    }

    /**
     * Fix gaps found in data.
     * <p/>
     * The interval between occurences {@code [0]} and {@code [1]} is considered to be the right one.
     * If a gap is detected and is larger than the interval, it will be filled with the content
     * of the last frame. However, if a gap smaller than the initial interval is detected,
     * an exception will be thrown as this method will not be able to fill it.
     * @param data Data to scan
     * @return Data with gaps filled if needed
     * @throws IllegalStateException If a gap that cannot be filled is found
     */
    List<Data> fixGaps(List<Data> data) throws IllegalStateException{
        def timeDiff = { LocalDateTime ldt1, LocalDateTime ldt2 ->
            def l2 = ldt2.toEpochSecond(ZoneOffset.UTC)
            def l1 = ldt1.toEpochSecond(ZoneOffset.UTC)
            l2 - l1
        }

        // Trivial case
        if (data.size() <= 2) return data

        // Build a copy of data. First element is to keep
        def dataCopy = [] as List<Data>
        dataCopy << data[0]

        // Time difference between the two first frames, as a reference
        long diff = timeDiff(data[0].dateTime, data[1].dateTime)

        for (int i = 1; i < data.size() - 1; i++) {
            def currentTimeDiff = timeDiff(data[i].dateTime, data[i+1].dateTime)
            if (currentTimeDiff != diff) {
                // Inconsistent diff between frames
                log.error("Inconsistent timeDiff at $i = ${data[i].dateTime} vs ${i+1} = ${data[i+1].dateTime}")
                if (currentTimeDiff < diff) {
                    // Smaller frame: bail out
                    throw new IllegalStateException("Frame with smaller time interval detected, cannot fix")
                }
                // There is a gap: we can fill it
                def missingFrames = currentTimeDiff / diff as int
                log.info("Filling the gap with previous data ($missingFrames missing frames)")
                missingFrames.times { dataCopy << data[i] }
            } else {
                // No diff error, keep data
                dataCopy << data[i]
            }
        }

        assert dataCopy.size() >= data.size()

        dataCopy
    }

    /**
     * Store the data in the given file, as CSV
     * @param data Data to store
     * @param path Path to the file. Will be overriden if not empty.
     */
    void store(List<Data> data, String path) {
        def csv = new File(path)
        log.info "Creating ${csv.absolutePath}"
        if (csv.exists()) {
            // Empty file
            log.info "Overriding content"
            def fos = new FileOutputStream(csv)
            fos.close()
        }

        csv << "#dateTime,open,close,low,high,volumeFrom,volumeTo\n"

        data.each { d ->
            def dt = fromLocalDateTime(d.dateTime)
            csv << "${dt},${d.opening},${d.closing},${d.low},${d.high},${d.volumeFrom},${d.volumeTo}\n"
        }

        log.info("Done writing ${data.size()} entries")
    }

    /**
     * Load data from a CSV file.
     * <p/>
     * Sample:
     * <pre>
     * #dateTime,open,close,low,high,volumeFrom,volumeTo
     * 2017-03-17T13:34:00,43.01,42.96,42.74,43.06,207.53,8927.73
     * 2017-03-17T13:35:00,42.96,42.74,42.74,42.96,94.26,4036.05
     * </pre>
     *
     * @return List of {@link Data}
     */
    List<Data> loadFromCsv(String path) {
        def csv = new File(path)
        def data = [] as List<Data>
        for (String line : csv) {
            if (line.startsWith('#')) continue
            def parts = line.split(',')
            def dt = toLocalDateTime(parts[0])
            data << new Data(dateTime: dt,
                    opening: parts[1] as BigDecimal,
                    closing: parts[2] as BigDecimal,
                    low: parts[3] as BigDecimal,
                    high: parts[4] as BigDecimal,
                    volumeFrom: parts[5] as BigDecimal,
                    volumeTo: parts[6] as BigDecimal,
            )
        }

        data
    }

    static void main(String[] args) {
        String dataFolder = args ? args[0] : '.'
        String csv = dataFolder + File.separator + CSV_FILE_PATH

        def rd = new RawData()

        println 'Reading data from CSV'
        def dataMap = rd.readFromCsv(csv)
        println "(${dataMap.size()} entries)"

        println 'Reading data from JSON'
        dataMap = rd.readFromJsonFiles(dataMap, dataFolder)
        println "(${dataMap.size()} entries)"

        println 'Sort data'
        def data = rd.sortDataMap(dataMap)

        println 'Checking coherence and filling gaps'
        data = rd.fixGaps(data)
        println "(${data.size()} entries)"

        println 'Store data'
        rd.store(data, csv)
    }
}
