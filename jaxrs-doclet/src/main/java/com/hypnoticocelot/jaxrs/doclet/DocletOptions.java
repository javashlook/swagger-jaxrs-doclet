package com.hypnoticocelot.jaxrs.doclet;

import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Iterables.*;
import static java.util.Arrays.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.hypnoticocelot.jaxrs.doclet.translator.AnnotationAwareTranslator;
import com.hypnoticocelot.jaxrs.doclet.translator.FirstNotNullTranslator;
import com.hypnoticocelot.jaxrs.doclet.translator.NameBasedTranslator;
import com.hypnoticocelot.jaxrs.doclet.translator.Translator;

public class DocletOptions {
    public static final String DEFAULT_SWAGGER_UI_ZIP_PATH = "n/a";
    private File outputDirectory;
    private String docBasePath = "http://localhost:8080";
    private String apiBasePath = "http://localhost:8080";
    private String swaggerUiZipPath = DEFAULT_SWAGGER_UI_ZIP_PATH;
    private String apiVersion = "0";
    private List<String> typesToTreatAsOpaque;
    
    // Premer 2014-07-28: stanasic ////////////////////////////////////////////////////////////////////////////////////
    private List<String> packagesToTreatAsOpaque;
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    private List<String> errorTags;
    private List<String> excludeAnnotationClasses;
    private boolean parseModels = true;
    private Recorder recorder = new ObjectMapperRecorder();
    private Translator translator;

    public DocletOptions() {
        excludeAnnotationClasses = new ArrayList<String>();
        excludeAnnotationClasses.add("javax.ws.rs.HeaderParam");
        excludeAnnotationClasses.add("javax.ws.rs.core.Context");
        errorTags = new ArrayList<String>();
        errorTags.add("errorResponse");   // swagger 1.1
        errorTags.add("responseMessage"); // swagger 1.2
        typesToTreatAsOpaque = new ArrayList<String>();
        typesToTreatAsOpaque.add("org.joda.time.DateTime");
        typesToTreatAsOpaque.add("java.util.UUID");
        
        // Premer 2014-07-28: stanasic ////////////////////////////////////////////////////////////////////////////////////
        typesToTreatAsOpaque.add("java.lang.Class");
        packagesToTreatAsOpaque = Lists.newArrayList("org.joda.time", "java.math");
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        
        translator = new FirstNotNullTranslator()
                .addNext(new AnnotationAwareTranslator()
                        .ignore("javax.xml.bind.annotation.XmlTransient")
                        .element("javax.xml.bind.annotation.XmlElement", "name")
                        .rootElement("javax.xml.bind.annotation.XmlRootElement", "name")
                )
                .addNext(new AnnotationAwareTranslator()
                        .ignore("com.fasterxml.jackson.annotation.JsonIgnore")
                        .element("com.fasterxml.jackson.annotation.JsonProperty", "value")
                        .rootElement("com.fasterxml.jackson.annotation.JsonRootName", "value")
                )
                .addNext(new NameBasedTranslator());
    }

    public static DocletOptions parse(String[][] options) {
        DocletOptions parsedOptions = new DocletOptions();
        for (String[] option : options) {
            if (option[0].equals("-d")) {
                parsedOptions.outputDirectory = new File(option[1]);
                checkArgument(parsedOptions.outputDirectory.isDirectory(), "Path after -d is expected to be a directory!");
            } else if (option[0].equals("-docBasePath")) {
                parsedOptions.docBasePath = option[1];
            } else if (option[0].equals("-apiBasePath")) {
                parsedOptions.apiBasePath = option[1];
            } else if (option[0].equals("-apiVersion")) {
                parsedOptions.apiVersion = option[1];
            } else if (option[0].equals("-swaggerUiZipPath")) {
                parsedOptions.swaggerUiZipPath = option[1];
            }
            else {
                if (option[0].equals("-excludeAnnotationClasses")) {
                    parsedOptions.excludeAnnotationClasses.addAll(asList(copyOfRange(option, 1, option.length)));
                }
                else if (option[0].equals("-disableModels")) {
                    parsedOptions.parseModels = false;
                }
                else if (option[0].equals("-errorTags")) {
                    parsedOptions.errorTags.addAll(asList(copyOfRange(option, 1, option.length)));
                }
                // Premer 2014-07-28: stanasic ////////////////////////////////////////////////////////////////////////////////////                
                else if (option[0].equals("-typesToTreatAsOpaque")) {
                    addOption(parsedOptions.typesToTreatAsOpaque, option);
                }
                else if (option[0].equals("-packagesToTreatAsOpaque")) {
                    addOption(parsedOptions.packagesToTreatAsOpaque, option);
                }
                ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////                
            }
        }
        return parsedOptions;
    }

    // Premer 2014-07-28: stanasic ////////////////////////////////////////////////////////////////////////////////////
    private static void addOption(Collection<String> elements, String[] option) {
        List<String> args = asList(copyOfRange(option, 1, option.length));
        if (args.size() > 0) {
            addAll(elements, splitElements(args.get(0)));
        }
    }
    
    private static Iterable<String> splitElements(String elementStr) {
        return Splitter.on(",").trimResults().omitEmptyStrings().split(elementStr);
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public String getDocBasePath() {
        return docBasePath;
    }

    public String getApiBasePath() {
        return apiBasePath;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public String getSwaggerUiZipPath() {
        return swaggerUiZipPath;
    }

    public List<String> getExcludeAnnotationClasses() {
        return excludeAnnotationClasses;
    }
    
    public List<String> getErrorTags() {
        return errorTags;
    }

    public List<String> getTypesToTreatAsOpaque() {
        return typesToTreatAsOpaque;
    }

    // Premer 2014-07-28: stanasic ////////////////////////////////////////////////////////////////////////////////////
    public List<String> getPackagesToTreatAsOpaque() {
        return packagesToTreatAsOpaque;
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    public boolean isParseModels() {
        return parseModels;
    }

    public Recorder getRecorder() {
        return recorder;
    }

    public DocletOptions setRecorder(Recorder recorder) {
        this.recorder = recorder;
        return this;
    }

    public Translator getTranslator() {
        return translator;
    }

    public DocletOptions setTranslator(Translator translator) {
        this.translator = translator;
        return this;
    }

}
