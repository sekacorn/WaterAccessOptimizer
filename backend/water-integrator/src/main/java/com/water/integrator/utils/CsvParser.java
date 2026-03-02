package com.water.integrator.utils;

import com.water.integrator.model.CommunityData;
import com.water.integrator.model.HydroData;
import com.water.integrator.model.InfrastructureData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Utility class for parsing CSV files containing water data.
 */
@Component
@Slf4j
public class CsvParser {

    private final GeometryFactory geometryFactory = new GeometryFactory();

    public List<HydroData> parseHydroDataFromCsv(InputStream inputStream, UUID userId) throws Exception {
        List<HydroData> dataList = new ArrayList<>();

        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim())) {

            for (CSVRecord record : csvParser) {
                HydroData data = new HydroData();
                data.setUserId(userId);
                data.setSource(record.get("source"));
                data.setDataType(record.get("data_type"));
                data.setLocationName(record.get("location_name"));
                data.setMeasurementValue(new BigDecimal(record.get("measurement_value")));
                data.setMeasurementUnit(record.get("measurement_unit"));

                // Parse coordinates
                double lat = Double.parseDouble(record.get("latitude"));
                double lon = Double.parseDouble(record.get("longitude"));
                Point point = geometryFactory.createPoint(new Coordinate(lon, lat));
                point.setSRID(4326);
                data.setLocation(point);

                // Parse date if present
                if (record.isMapped("measurement_date") && !record.get("measurement_date").isEmpty()) {
                    data.setMeasurementDate(LocalDateTime.parse(record.get("measurement_date"),
                            DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                }

                dataList.add(data);
            }
        }

        log.info("Parsed {} hydro data records from CSV", dataList.size());
        return dataList;
    }

    public List<CommunityData> parseCommunityDataFromCsv(InputStream inputStream, UUID userId) throws Exception {
        List<CommunityData> dataList = new ArrayList<>();

        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim())) {

            for (CSVRecord record : csvParser) {
                CommunityData data = new CommunityData();
                data.setUserId(userId);
                data.setCommunityName(record.get("community_name"));
                data.setPopulation(Integer.parseInt(record.get("population")));
                data.setWaterAccessLevel(record.get("water_access_level"));
                data.setSource(record.get("source"));

                // Parse coordinates
                double lat = Double.parseDouble(record.get("latitude"));
                double lon = Double.parseDouble(record.get("longitude"));
                Point point = geometryFactory.createPoint(new Coordinate(lon, lat));
                point.setSRID(4326);
                data.setLocation(point);

                dataList.add(data);
            }
        }

        log.info("Parsed {} community data records from CSV", dataList.size());
        return dataList;
    }

    public List<InfrastructureData> parseInfrastructureDataFromCsv(InputStream inputStream, UUID userId) throws Exception {
        List<InfrastructureData> dataList = new ArrayList<>();

        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim())) {

            for (CSVRecord record : csvParser) {
                InfrastructureData data = new InfrastructureData();
                data.setUserId(userId);
                data.setFacilityType(record.get("facility_type"));
                data.setFacilityName(record.get("facility_name"));
                data.setCapacity(new BigDecimal(record.get("capacity")));
                data.setCapacityUnit(record.get("capacity_unit"));
                data.setOperationalStatus(record.get("operational_status"));

                // Parse coordinates
                double lat = Double.parseDouble(record.get("latitude"));
                double lon = Double.parseDouble(record.get("longitude"));
                Point point = geometryFactory.createPoint(new Coordinate(lon, lat));
                point.setSRID(4326);
                data.setLocation(point);

                dataList.add(data);
            }
        }

        log.info("Parsed {} infrastructure data records from CSV", dataList.size());
        return dataList;
    }
}
