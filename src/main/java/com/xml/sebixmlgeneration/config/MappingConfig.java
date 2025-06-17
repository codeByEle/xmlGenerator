package com.xml.sebixmlgeneration.config;

import java.util.List;

public class MappingConfig {
	 private String rootElement;
	    private List<Element> elements;

	    public String getRootElement() { return rootElement; }
	    public void setRootElement(String rootElement) { this.rootElement = rootElement; }

	    public List<Element> getElements() { return elements; }
	    public void setElements(List<Element> elements) { this.elements = elements; }

	    public static class Element {
	        private String name;
	        private String type; // object, list, value
	        private String source;
	        private String itemName;
	        private List<Element> fields;

	        public String getName() { return name; }
	        public void setName(String name) { this.name = name; }

	        public String getType() { return type; }
	        public void setType(String type) { this.type = type; }

	        public String getSource() { return source; }
	        public void setSource(String source) { this.source = source; }

	        public String getItemName() { return itemName; }
	        public void setItemName(String itemName) { this.itemName = itemName; }

	        public List<Element> getFields() { return fields; }
	        public void setFields(List<Element> fields) { this.fields = fields; }
	    }
}
