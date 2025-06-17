package com.xml.sebixmlgeneration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;
import com.xml.sebixmlgeneration.config.CsvDbConfig;
import com.xml.sebixmlgeneration.config.MappingConfig;


@RestController
@RequestMapping("/generate-xml")
public class XmlGeneratorController {

    private static final Logger logger = LogManager.getLogger(XmlGeneratorController.class);
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final ObjectMapper jsonMapper = new ObjectMapper();

    @GetMapping("/{type}/{version}")
    public ResponseEntity<InputStreamResource> generateXmlByVersion(@PathVariable String type, @PathVariable String version) throws Exception {
        return generateXml(type, version, null, null);
    }

    @GetMapping("/{type}/{fromDate}/{toDate}")
    public ResponseEntity<InputStreamResource> generateXmlByDateRange(@PathVariable String type, @PathVariable String fromDate, @PathVariable String toDate) throws Exception {
        return generateXml(type, null, fromDate, toDate);
    }

    private ResponseEntity<InputStreamResource> generateXml(String type, String version, String fromDate, String toDate) throws Exception {
        File configFile = new File(System.getProperty("config.dir", "./config") + "/config.json");
        Map<String, CsvDbConfig> configMap = jsonMapper.readValue(configFile, jsonMapper.getTypeFactory().constructMapType(Map.class, String.class, CsvDbConfig.class));

        if (!configMap.containsKey(type)) {
            logger.error("No config found for type: " + type);
            throw new IllegalArgumentException("No config found for: " + type);
        }

        CsvDbConfig config = configMap.get(type);
        MappingConfig mapping = yamlMapper.readValue(new File(config.getMappingFile()), MappingConfig.class);

        List<Map<String, Object>> data;
        String outputFile;

        if ("csv".equalsIgnoreCase(config.getSourceType())) {
            String filename = config.getCsvDir() + type + "_" + (version != null ? version : fromDate + "_" + toDate) + ".csv";
            logger.info("Reading data from CSV: " + filename);
            data = readFromCsv(filename);
            outputFile = config.getOutputPrefix() + (version != null ? version : fromDate + "_" + toDate);
        } else {
            logger.info("Reading data from DB for type: " + type);
            data = readFromDb(config, version, fromDate, toDate);
            outputFile = config.getOutputPrefix() + (version != null ? version : fromDate + "_" + toDate);
        }

        File xmlFile = File.createTempFile(outputFile, ".xml");
        writeXml(mapping, data, xmlFile, config.getSchemaPath());

        return buildResponse(xmlFile, outputFile + ".xml");
    }

    private List<Map<String, Object>> readFromCsv(String path) throws Exception {
        try (Reader reader = Files.newBufferedReader(Paths.get(path));
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            List<Map<String, Object>> records = new ArrayList<>();
            for (CSVRecord record : parser) {
                Map<String, Object> row = parser.getHeaderMap().keySet().stream()
                        .collect(Collectors.toMap(h -> h, record::get));
                records.add(row);
            }
            return records;
        }
    }

    private List<Map<String, Object>> readFromDb(CsvDbConfig config, String version, String fromDate, String toDate) throws Exception {
        Class.forName(config.getDriverClass());
        List<Map<String, Object>> result;

        try (Connection conn = DriverManager.getConnection(config.getDbUrl(), config.getDbUser(), config.getDbPassword());
             PreparedStatement ps = prepareQuery(conn, config.getBaseQuery(), version, fromDate, toDate);
             ResultSet rs = ps.executeQuery()) {

            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            result = new ArrayList<>();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                result.add(row);
            }
        }
        return result;
    }

    private PreparedStatement prepareQuery(Connection conn, String query, String version, String fromDate, String toDate) throws SQLException {
        if (version != null) {
            query = query.replace(":version", "?");
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, version);
            return ps;
        } else if (fromDate != null && toDate != null) {
            query = query.replace(":fromDate", "?").replace(":toDate", "?");
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, fromDate);
            ps.setString(2, toDate);
            return ps;
        } else {
            throw new IllegalArgumentException("Invalid parameters for query");
        }
    }

    private void writeXml(MappingConfig config, List<Map<String, Object>> data, File file, String schema) throws Exception {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            XMLStreamWriter writer = factory.createXMLStreamWriter(fos, "UTF-8");

            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeStartElement(config.getRootElement());
            writer.writeNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
            writer.writeAttribute("xsi:noNamespaceSchemaLocation", schema);

            writeElement(writer, config.getElements(), data);

            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
        }
    }

    private void writeElement(XMLStreamWriter writer, List<MappingConfig.Element> elements, List<Map<String, Object>> rows) throws Exception {
        for (MappingConfig.Element element : elements) {
            if ("list".equals(element.getType())) {
                writer.writeStartElement(element.getName());
                for (Map<String, Object> row : rows) {
                    writer.writeStartElement(element.getItemName() != null ? element.getItemName() : "Item");
                    writeElement(writer, element.getFields(), Collections.singletonList(row));
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            } else if ("object".equals(element.getType())) {
                writer.writeStartElement(element.getName());
                writeElement(writer, element.getFields(), rows);
                writer.writeEndElement();
            } else {
                for (Map<String, Object> row : rows) {
                    Object value = row.getOrDefault(element.getSource(), "");
                    writer.writeStartElement(element.getName());
                    writer.writeCharacters(value != null ? value.toString() : "");
                    writer.writeEndElement();
                }
            }
        }
    }

    private ResponseEntity<InputStreamResource> buildResponse(File file, String filename) throws IOException {
        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_XML)
                .body(resource);
    }
}