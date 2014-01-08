/*
 * Copyright (C) 2013 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.as.rest.v2.model;

import com.intel.mtwilson.jersey.DocumentCollection;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.ArrayList;
import java.util.List;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * 
 * @author jbuhacoff
 */
@JacksonXmlRootElement(localName="file_collection")
public class FileCollection extends DocumentCollection<File> {
    private final ArrayList<File> files = new ArrayList<File>();
    
    @JsonSerialize(include=JsonSerialize.Inclusion.ALWAYS) // jackson 1.9
    @JsonInclude(JsonInclude.Include.ALWAYS)                // jackson 2.0
    @JacksonXmlElementWrapper(localName="files")
    @JacksonXmlProperty(localName="file")    
    public List<File> getFiles() { return files; }
    
    @Override
    public List<File> getDocuments() {
        return getFiles();
    }
    
    
}
