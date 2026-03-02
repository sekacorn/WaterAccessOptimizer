package com.water.integrator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.water.integrator.model.CommunityData;
import com.water.integrator.model.HydroData;
import com.water.integrator.model.InfrastructureData;
import com.water.integrator.repository.CommunityDataRepository;
import com.water.integrator.repository.HydroDataRepository;
import com.water.integrator.repository.InfrastructureDataRepository;
import com.water.integrator.utils.CsvParser;
import com.water.integrator.utils.GeoJsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Service for integrating and processing water data from multiple sources.
 * Supports CSV, JSON, and GeoJSON formats.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataIntegrationService {

    private final HydroDataRepository hydroDataRepository;
    private final CommunityDataRepository communityDataRepository;
    private final InfrastructureDataRepository infrastructureDataRepository;
    private final CsvParser csvParser;
    private final GeoJsonParser geoJsonParser;
    private final ObjectMapper objectMapper;

    /**
     * Process and save hydrological data from uploaded file
     */
    @Transactional
    public List<HydroData> processHydroData(MultipartFile file, UUID userId) throws Exception {
        String filename = file.getOriginalFilename();
        List<HydroData> dataList;

        if (filename != null) {
            if (filename.endsWith(".csv")) {
                dataList = csvParser.parseHydroDataFromCsv(file.getInputStream(), userId);
            } else if (filename.endsWith(".json") || filename.endsWith(".geojson")) {
                dataList = geoJsonParser.parseHydroDataFromJson(file.getInputStream(), userId);
            } else {
                throw new IllegalArgumentException("Unsupported file format. Please upload CSV, JSON, or GeoJSON.");
            }
        } else {
            throw new IllegalArgumentException("File name is null");
        }

        log.info("Saving {} hydro data records", dataList.size());
        return hydroDataRepository.saveAll(dataList);
    }

    /**
     * Process and save community data from uploaded file
     */
    @Transactional
    public List<CommunityData> processCommunityData(MultipartFile file, UUID userId) throws Exception {
        String filename = file.getOriginalFilename();
        List<CommunityData> dataList;

        if (filename != null) {
            if (filename.endsWith(".csv")) {
                dataList = csvParser.parseCommunityDataFromCsv(file.getInputStream(), userId);
            } else if (filename.endsWith(".json") || filename.endsWith(".geojson")) {
                dataList = geoJsonParser.parseCommunityDataFromJson(file.getInputStream(), userId);
            } else {
                throw new IllegalArgumentException("Unsupported file format. Please upload CSV, JSON, or GeoJSON.");
            }
        } else {
            throw new IllegalArgumentException("File name is null");
        }

        log.info("Saving {} community data records", dataList.size());
        return communityDataRepository.saveAll(dataList);
    }

    /**
     * Process and save infrastructure data from uploaded file
     */
    @Transactional
    public List<InfrastructureData> processInfrastructureData(MultipartFile file, UUID userId) throws Exception {
        String filename = file.getOriginalFilename();
        List<InfrastructureData> dataList;

        if (filename != null) {
            if (filename.endsWith(".csv")) {
                dataList = csvParser.parseInfrastructureDataFromCsv(file.getInputStream(), userId);
            } else if (filename.endsWith(".json") || filename.endsWith(".geojson")) {
                dataList = geoJsonParser.parseInfrastructureDataFromJson(file.getInputStream(), userId);
            } else {
                throw new IllegalArgumentException("Unsupported file format. Please upload CSV, JSON, or GeoJSON.");
            }
        } else {
            throw new IllegalArgumentException("File name is null");
        }

        log.info("Saving {} infrastructure data records", dataList.size());
        return infrastructureDataRepository.saveAll(dataList);
    }

    /**
     * Retrieve hydro data with caching
     */
    @Cacheable(value = "hydroData", key = "#userId")
    public List<HydroData> getHydroData(UUID userId) {
        if (userId != null) {
            return hydroDataRepository.findByUserId(userId);
        }
        return hydroDataRepository.findAll();
    }

    /**
     * Retrieve community data with caching
     */
    @Cacheable(value = "communityData", key = "#userId")
    public List<CommunityData> getCommunityData(UUID userId) {
        if (userId != null) {
            return communityDataRepository.findByUserId(userId);
        }
        return communityDataRepository.findAll();
    }

    /**
     * Retrieve infrastructure data with caching
     */
    @Cacheable(value = "infrastructureData", key = "#userId")
    public List<InfrastructureData> getInfrastructureData(UUID userId) {
        if (userId != null) {
            return infrastructureDataRepository.findByUserId(userId);
        }
        return infrastructureDataRepository.findAll();
    }
}
