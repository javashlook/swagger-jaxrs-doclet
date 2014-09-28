package com.hypnoticocelot.jaxrs.doclet.parser;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hypnoticocelot.jaxrs.doclet.DocletOptions;
import com.hypnoticocelot.jaxrs.doclet.model.Model;
import com.hypnoticocelot.jaxrs.doclet.model.Property;
import com.hypnoticocelot.jaxrs.doclet.translator.Translator;
import com.sun.javadoc.*;

import java.util.*;

import static com.google.common.collect.Collections2.filter;

/**
 * 
 */
/**
 * 
 */
public class ApiModelParser {

    private final DocletOptions options;
    private final Translator translator;
    private final Type rootType;
    private final Set<Model> models;

    public ApiModelParser(DocletOptions options, Translator translator, Type rootType) {
        this.options = options;
        this.translator = translator;
        this.rootType = rootType;
        this.models = new LinkedHashSet<Model>();
    }

    public Set<Model> parse() {
        parseModel(rootType);
        return models;
    }

    private void parseModel(Type type) {
        boolean isPrimitive = /* type.isPrimitive()? || */ AnnotationHelper.isPrimitive(type);
        boolean isJavaxType = type.qualifiedTypeName().startsWith("javax.");
        boolean isBaseObject = type.qualifiedTypeName().equals("java.lang.Object");
        // Premer 2014-07-28: stanasic ////////////////////////////////////////////////////////////////////////////////////
        boolean isTypeToTreatAsOpaque = options.getTypesToTreatAsOpaque().contains(type.qualifiedTypeName())
                        || startsWithAny(options.getPackagesToTreatAsOpaque(), type.qualifiedTypeName());
        ClassDoc classDoc = type.asClassDoc();
        if (isPrimitive || isJavaxType || isBaseObject || isTypeToTreatAsOpaque || classDoc == null) {
            // System.out.println("ApiModelParser.parseModel(): Skipping " + type.qualifiedTypeName());
            return;
        }
        if (alreadyStoredType(type)) {
            return;
        }
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        // Premer 2014-09-28: stanasic ////////////////////////////////////////////////////////////////////////////////////
        Map<String, TypeHolder> types = findReferencedTypes(classDoc);
        Map<String, Property> elements = findReferencedElements(types);
        if (!elements.isEmpty()) {
            models.add(new Model(translator.typeName(type).value(), elements));
            parseNestedModels(types.values());
        }
        
        System.out.println("ApiModelParser.parseModel(): " + type.qualifiedTypeName());
    }
    
    // Premer 2014-07-28: stanasic ////////////////////////////////////////////////////////////////////////////////////
    private boolean startsWithAny(List<String> packagesToTreatAsOpaque, String qualifiedTypeName) {
        for (String packageName : packagesToTreatAsOpaque) {
            if (!Strings.isNullOrEmpty(packageName) && qualifiedTypeName.startsWith(packageName)) {
                return true;
            }
        }
        return false;
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Premer 2014-09-28: stanasic ////////////////////////////////////////////////////////////////////////////////////
    static class TypeHolder {
        Type type;
        String description;
        TypeHolder(Type type, String description) {
            super();
            this.type = type;
            this.description = description;
        }
    }
    
    private Map<String, TypeHolder> findReferencedTypes(ClassDoc classDoc) {
        Map<String, TypeHolder> elements = Maps.newLinkedHashMap();

        // Premer 2014-09-28: stanasic ////////////////////////////////////////////////////////////////////////////////////
        List<FieldDoc> fieldDocs = Lists.newArrayList();
        allFields(classDoc, fieldDocs);
        
        for (FieldDoc field : fieldDocs) {
            if (field.isStatic())
                continue;

            String name = translator.fieldName(field).value();
            if (name != null && !elements.containsKey(name)) {
                elements.put(name, new TypeHolder(field.type(), field.getRawCommentText()));
            }
        }
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        MethodDoc[] methodDocs = classDoc.methods();
        if (methodDocs != null) {
            for (MethodDoc method : methodDocs) {
                if (method.isStatic())
                    continue;

                String name = translator.methodName(method).value();
                if (name != null && !elements.containsKey(name)) {
                    Tag[] rt = method.tags("@return");
                    String rtc = (rt.length > 0) ? rt[0].text() : "";
                    elements.put(name, new TypeHolder(method.returnType(), rtc));
                }
            }
        }
        return elements;
    }

    private void allFields(ClassDoc classDoc, List<FieldDoc> fieldDocs) {
        if (classDoc == null) {
            return;
        }
        fieldDocs.addAll(Arrays.asList(classDoc.fields()));
        allFields(classDoc.superclass(), fieldDocs);
    }
    
    private Map<String, Property> findReferencedElements(Map<String, TypeHolder> types) {
        Map<String, Property> elements = new HashMap<String, Property>();
        for (Map.Entry<String, TypeHolder> entry : types.entrySet()) {
            String typeName = entry.getKey();
            TypeHolder typeHolder = entry.getValue();
            Type type = typeHolder.type;
            ClassDoc typeClassDoc = type.asClassDoc();

            Type containerOf = getTypeArgument(type);
            String containerTypeOf = containerOf == null ? null : translator.typeName(containerOf).value();

            String propertyName = translator.typeName(type).value();
            Property property;
            if (typeClassDoc != null && typeClassDoc.isEnum()) {
                property = new Property(typeClassDoc.enumConstants(), typeHolder.description);
            } else {
                property = new Property(propertyName, typeHolder.description, containerTypeOf);
            }
            elements.put(typeName, property);
        }
        return elements;
    }

    private void parseNestedModels(Collection<TypeHolder> types) {
        for (TypeHolder type : types) {
            parseModel(type.type);
            Type pt = getTypeArgument(type.type);
            if (pt != null) {
                parseModel(pt);
            }
        }
    }

    public static Type getTypeArgument(Type type) {
        Type result = null;
        ParameterizedType pt = type.asParameterizedType();
        if (pt != null) {
            Type[] typeArgs = pt.typeArguments();
            if (typeArgs != null && typeArgs.length > 0) {
                result = typeArgs[0];
            }
        }
        return result;
    }

    private boolean alreadyStoredType(final Type type) {
        return filter(models, new Predicate<Model>() {
            @Override
            public boolean apply(Model model) {
                return model.getId().equals(translator.typeName(type).value());
            }
        }).size() > 0;
    }

}
