package com.xml.sebixmlgeneration.config;

public class CsvDbConfig {
	 private String sourceType; // csv or db
	    private String csvDir;
	    private String dbUrl;
	    private String dbUser;
	    private String dbPassword;
	    private String driverClass;
	    private String baseQuery;
	    private String outputPrefix;
	    private String schemaPath;
	    private String mappingFile;

	    // Getters and setters
	    public String getSourceType() { return sourceType; }
	    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

	    public String getCsvDir() { return csvDir; }
	    public void setCsvDir(String csvDir) { this.csvDir = csvDir; }

	    public String getDbUrl() { return dbUrl; }
	    public void setDbUrl(String dbUrl) { this.dbUrl = dbUrl; }

	    public String getDbUser() { return dbUser; }
	    public void setDbUser(String dbUser) { this.dbUser = dbUser; }

	    public String getDbPassword() { return dbPassword; }
	    public void setDbPassword(String dbPassword) { this.dbPassword = dbPassword; }

	    public String getDriverClass() { return driverClass; }
	    public void setDriverClass(String driverClass) { this.driverClass = driverClass; }

	    public String getBaseQuery() { return baseQuery; }
	    public void setBaseQuery(String baseQuery) { this.baseQuery = baseQuery; }

	    public String getOutputPrefix() { return outputPrefix; }
	    public void setOutputPrefix(String outputPrefix) { this.outputPrefix = outputPrefix; }

	    public String getSchemaPath() { return schemaPath; }
	    public void setSchemaPath(String schemaPath) { this.schemaPath = schemaPath; }

	    public String getMappingFile() { return mappingFile; }
	    public void setMappingFile(String mappingFile) { this.mappingFile = mappingFile; }
}
