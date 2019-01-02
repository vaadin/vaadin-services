/*
 * Copyright 2000-2019 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.connect.plugin.generator;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.javadoc.JavadocBlockTag;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.utils.SourceRoot;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.slf4j.LoggerFactory;

import com.vaadin.connect.VaadinService;
import com.vaadin.connect.oauth.AnonymousAllowed;

/**
 * Java parser class which scans for all {@link VaadinService} classes and
 * produces OpenApi json.
 */
class OpenApiParser {
  private static final List<String> NUMBER_TYPES = Arrays.asList("int",
      "integer", "short", "long", "double", "float");
  private static final List<String> STRING_TYPES = Arrays.asList("string",
      "char");
  private static final List<String> BOOLEAN_TYPES = Collections
      .singletonList("boolean");
  private static final List<String> MAP_TYPES = Arrays.asList("map", "hashmap",
      "hashtable", "treemap", "sortedmap");
  private static final List<String> COLLECTION_TYPES = Arrays.asList(
      "collection", "list", "arraylist", "linkedlist", "set", "hashset",
      "sortedset", "treeset");
  private static final String VAADIN_CONNECT_JWT_SECURITY_SCHEME = "vaadin-connect-jwt";

  private List<Path> javaSourcePaths = new ArrayList<>();
  private OpenApiConfiguration configuration;
  private Set<String> usedSchemas;
  private Map<String, String> servicesJavadoc;
  private Map<String, Schema> nonServiceSchemas;
  private OpenAPI openApiModel;

  void addSourcePath(Path sourcePath) {
    if (sourcePath == null) {
      throw new IllegalArgumentException(
          "Java source path must be a valid directory");
    }
    if (!sourcePath.toFile().exists()) {
      throw new IllegalArgumentException("Java source path doesn't exist");
    }
    this.javaSourcePaths.add(sourcePath);
  }

  void setOpenApiConfiguration(OpenApiConfiguration configuration) {
    this.configuration = configuration;
  }

  OpenAPI getOpenApi() {
    if (openApiModel == null) {
      init();
    }
    return openApiModel;
  }

  OpenAPI generateOpenApi() {
    init();
    return openApiModel;
  }

  private void init() {
    if (javaSourcePaths == null || configuration == null) {
      throw new IllegalStateException(
          "Java source path and configuration should not be null");
    }
    openApiModel = createBasicModel();
    nonServiceSchemas = new HashMap<>();
    usedSchemas = new HashSet<>();
    servicesJavadoc = new HashMap<>();

    ParserConfiguration parserConfiguration = new ParserConfiguration()
        .setSymbolResolver(new JavaSymbolSolver(
            new CombinedTypeSolver(new ReflectionTypeSolver(false))));
    javaSourcePaths.stream()
        .map(path -> new SourceRoot(path, parserConfiguration))
        .forEach(this::parseSourceRoot);

    for (String s : usedSchemas) {
      Schema schema = nonServiceSchemas.get(s);
      if (schema != null) {
        openApiModel.getComponents().addSchemas(s, schema);
      }
    }
    addTagsInformation();
  }

  private void parseSourceRoot(SourceRoot sourceRoot) {
    try {
      sourceRoot.parse("", this::process);
    } catch (Exception e) {
      LoggerFactory.getLogger(OpenApiParser.class).error(e.getMessage(), e);
      throw new IllegalStateException(String.format(
          "Can't parse the java files in the source root '%s'", sourceRoot), e);
    }
  }

  private void addTagsInformation() {
    for (Map.Entry<String, String> serviceJavadoc : servicesJavadoc
        .entrySet()) {
      Tag tag = new Tag();
      tag.name(serviceJavadoc.getKey());
      tag.description(serviceJavadoc.getValue());
      openApiModel.addTagsItem(tag);
    }
  }

  private OpenAPI createBasicModel() {
    OpenAPI openAPI = new OpenAPI();

    Info info = new Info();
    info.setTitle(configuration.getApplicationTitle());
    info.setVersion(configuration.getApplicationApiVersion());
    openAPI.setInfo(info);

    Paths paths = new Paths();
    openAPI.setPaths(paths);

    Server server = new Server();
    server.setUrl(configuration.getServerUrl());
    server.setDescription(configuration.getServerDescription());
    openAPI.setServers(Collections.singletonList(server));
    Components components = new Components();
    SecurityScheme vaadinConnectJwtScheme = new SecurityScheme();
    vaadinConnectJwtScheme.type(SecurityScheme.Type.HTTP);
    vaadinConnectJwtScheme.scheme("bearer");
    vaadinConnectJwtScheme.bearerFormat("JWT");
    components.addSecuritySchemes(VAADIN_CONNECT_JWT_SECURITY_SCHEME,
        vaadinConnectJwtScheme);
    openAPI.components(components);
    return openAPI;
  }

  @SuppressWarnings("squid:S1172")
  private SourceRoot.Callback.Result process(Path localPath, Path absolutePath,
      ParseResult<CompilationUnit> result) {
    result.ifSuccessful(compilationUnit -> compilationUnit.getPrimaryType()
        .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
        .map(BodyDeclaration::asClassOrInterfaceDeclaration)
        .map(this::appendNestedClasses).orElse(Collections.emptyList())
        .forEach(this::parseClass));
    return SourceRoot.Callback.Result.DONT_SAVE;
  }

  private Collection<ClassOrInterfaceDeclaration> appendNestedClasses(
      ClassOrInterfaceDeclaration topLevelClass) {
    TreeSet<ClassOrInterfaceDeclaration> nestedClasses = topLevelClass
        .getMembers().stream()
        .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
        .map(BodyDeclaration::asClassOrInterfaceDeclaration)
        .collect(Collectors.toCollection(() -> new TreeSet<>(
            Comparator.comparing(NodeWithSimpleName::getNameAsString))));
    nestedClasses.add(topLevelClass);
    return nestedClasses;
  }

  private void parseClass(ClassOrInterfaceDeclaration classDeclaration) {
    String className = classDeclaration.getNameAsString();
    if (!classDeclaration.isAnnotationPresent(VaadinService.class)) {
      nonServiceSchemas.put(className, parseClassAsSchema(classDeclaration));
      return;
    }
    classDeclaration.getJavadoc().ifPresent(javadoc -> servicesJavadoc
        .put(className, javadoc.getDescription().toText()));

    Map<String, PathItem> pathItems = createPathItems(classDeclaration);

    for (Map.Entry<String, PathItem> entry : pathItems.entrySet()) {
      String methodName = entry.getKey();
      PathItem pathItem = entry.getValue();
      String pathName = "/" + className + "/" + methodName;
      pathItem.readOperationsMap()
          .forEach((httpMethod, operation) -> operation.setOperationId(
              String.join("_", className, methodName, httpMethod.name())));
      openApiModel.getPaths().addPathItem(pathName, pathItem);
    }
  }

  private Schema parseClassAsSchema(
      TypeDeclaration<ClassOrInterfaceDeclaration> typeDeclaration) {
    Schema schema = new ObjectSchema();
    typeDeclaration.getJavadoc().ifPresent(
        javadoc -> schema.description(javadoc.getDescription().toText()));
    for (FieldDeclaration field : typeDeclaration.getFields()) {
      if (field.isTransient()) {
        continue;
      }
      field.getVariables()
          .forEach(variableDeclarator -> schema.addProperties(
              variableDeclarator.getNameAsString(),
              parseTypeToSchema(variableDeclarator.getType())));
    }
    return schema;
  }

  private boolean isReservedWord(String word) {
    return word != null && VaadinConnectJsGenerator.JS_RESERVED_WORDS
        .contains(word.toLowerCase());
  }

  private Map<String, PathItem> createPathItems(
      ClassOrInterfaceDeclaration typeDeclaration) {
    Map<String, PathItem> pathItems = new TreeMap<>();

    if (isReservedWord(typeDeclaration.getNameAsString())) {
      throw new IllegalStateException(
          "The service class name '" + typeDeclaration.getNameAsString()
              + "' is a JavaScript reserved word");
    }

    for (MethodDeclaration methodDeclaration : typeDeclaration.getMethods()) {
      if (!methodDeclaration.isPublic()
          || methodDeclaration.isAnnotationPresent(DenyAll.class)) {
        continue;
      }
      String methodName = methodDeclaration.getNameAsString();

      if (isReservedWord(methodName)) {
        throw new IllegalStateException("The method name '" + methodName
            + "' in the service class '" + typeDeclaration.getNameAsString()
            + "' is a JavaScript reserved word");
      }

      Operation post = createPostOperation(methodDeclaration,
          requiresAuthentication(typeDeclaration, methodDeclaration));

      if (methodDeclaration.getParameters().isNonEmpty()) {
        post.setRequestBody(createRequestBody(methodDeclaration));
      }

      ApiResponses responses = createApiResponses(methodDeclaration);
      post.setResponses(responses);
      post.tags(Collections.singletonList(typeDeclaration.getNameAsString()));
      PathItem pathItem = new PathItem().post(post);
      pathItems.put(methodName, pathItem);
    }
    return pathItems;
  }

  private boolean requiresAuthentication(
      ClassOrInterfaceDeclaration typeDeclaration,
      MethodDeclaration methodDeclaration) {
    if (hasSecurityAnnotation(methodDeclaration)) {
      return !methodDeclaration.isAnnotationPresent(AnonymousAllowed.class)
          || methodDeclaration.isAnnotationPresent(DenyAll.class);
    } else if (hasSecurityAnnotation(typeDeclaration)) {
      return !typeDeclaration.isAnnotationPresent(AnonymousAllowed.class);
    }
    return true;
  }

  private boolean hasSecurityAnnotation(NodeWithAnnotations<?> node) {
    return node.isAnnotationPresent(AnonymousAllowed.class)
        || node.isAnnotationPresent(PermitAll.class)
        || node.isAnnotationPresent(DenyAll.class)
        || node.isAnnotationPresent(RolesAllowed.class);
  }

  private Operation createPostOperation(MethodDeclaration methodDeclaration,
      boolean requiresAuthentication) {
    Operation post = new Operation();
    if (requiresAuthentication) {
      SecurityRequirement securityItem = new SecurityRequirement();
      securityItem.addList(VAADIN_CONNECT_JWT_SECURITY_SCHEME);
      post.addSecurityItem(securityItem);
    }

    methodDeclaration.getJavadoc().ifPresent(
        javadoc -> post.setDescription(javadoc.getDescription().toText()));
    return post;
  }

  private ApiResponses createApiResponses(MethodDeclaration methodDeclaration) {
    ApiResponse successfulResponse = createApiSuccessfulResponse(
        methodDeclaration);
    ApiResponses responses = new ApiResponses();
    responses.addApiResponse("200", successfulResponse);
    return responses;
  }

  private ApiResponse createApiSuccessfulResponse(
      MethodDeclaration methodDeclaration) {
    Content successfulContent = new Content();
    // "description" is a REQUIRED property of Response
    ApiResponse successfulResponse = new ApiResponse().description("");
    methodDeclaration.getJavadoc().ifPresent(javadoc -> {
      for (JavadocBlockTag blockTag : javadoc.getBlockTags()) {
        if (blockTag.getType() == JavadocBlockTag.Type.RETURN) {
          successfulResponse
              .setDescription("Return " + blockTag.getContent().toText());
        }
      }
    });
    if (!methodDeclaration.getType().isVoidType()) {
      MediaType mediaItem = createReturnMediaType(methodDeclaration);
      successfulContent.addMediaType("application/json", mediaItem);
      successfulResponse.content(successfulContent);
    }
    return successfulResponse;
  }

  private MediaType createReturnMediaType(MethodDeclaration methodDeclaration) {
    MediaType mediaItem = new MediaType();
    Type methodReturnType = methodDeclaration.getType();
    mediaItem.schema(parseTypeToSchema(methodReturnType));
    return mediaItem;
  }

  private RequestBody createRequestBody(MethodDeclaration methodDeclaration) {
    Map<String, String> paramsDescription = new HashMap<>();
    methodDeclaration.getJavadoc().ifPresent(javadoc -> {
      for (JavadocBlockTag blockTag : javadoc.getBlockTags()) {
        if (blockTag.getType() == JavadocBlockTag.Type.PARAM) {
          paramsDescription.put(blockTag.getName().orElse(""),
              blockTag.getContent().toText());
        }
      }
    });

    RequestBody requestBody = new RequestBody();
    Content requestBodyContent = new Content();
    requestBody.content(requestBodyContent);
    MediaType requestBodyObject = new MediaType();
    requestBodyContent.addMediaType("application/json", requestBodyObject);
    Schema requestSchema = new ObjectSchema();
    requestBodyObject.schema(requestSchema);
    methodDeclaration.getParameters().forEach(parameter -> {
      Schema paramSchema = parseTypeToSchema(parameter.getType());
      paramSchema.description(paramsDescription.get(parameter.getNameAsString()));

      String name = (isReservedWord(parameter.getNameAsString()) ? "_" : "")
          .concat(parameter.getNameAsString());
      requestSchema.addProperties(name, paramSchema);
    });
    return requestBody;
  }

  private Schema parseTypeToSchema(Type javaType) {
    if (javaType.isArrayType()) {
      return createArraySchema(javaType);
    }
    String typeName = getTypeName(javaType).toLowerCase(Locale.ENGLISH);
    if (NUMBER_TYPES.contains(typeName)) {
      return new NumberSchema();
    } else if (STRING_TYPES.contains(typeName)) {
      return new StringSchema();
    } else if (COLLECTION_TYPES.contains(typeName)) {
      return createArraySchema(javaType);
    } else if (BOOLEAN_TYPES.contains(typeName)) {
      return new BooleanSchema();
    } else if (MAP_TYPES.contains(typeName)) {
      return createMapSchema(javaType);
    }
    return createUserBeanSchema(javaType);
  }

  private Schema createUserBeanSchema(Type javaType) {
    String userType = getTypeName(javaType);
    usedSchemas.add(userType);
    return new ObjectSchema().$ref(userType);
  }

  private String getTypeName(Type javaType) {
    String typeName;
    if (javaType.isClassOrInterfaceType()) {
      typeName = javaType.asClassOrInterfaceType().getNameAsString();
    } else {
      typeName = javaType.asString();
    }
    return typeName;
  }

  private Schema createMapSchema(Type javaType) {
    Schema mapSchema = new MapSchema();
    Type mapValueType = (Type) javaType.getChildNodes().get(2);
    mapSchema.additionalProperties(parseTypeToSchema(mapValueType));
    return mapSchema;
  }

  private Schema createArraySchema(Type type) {
    ArraySchema array = new ArraySchema();
    if (type.isArrayType()) {
      // The first child of type "int[]" is its actual type
      Type arrayType = (Type) type.getChildNodes().get(0);
      array.items(parseTypeToSchema(arrayType));
    } else {
      // Child nodes of List<String> are: List and String
      // So we need the second one
      Type arrayType = (Type) type.getChildNodes().get(1);
      array.items(parseTypeToSchema(arrayType));
    }
    return array;
  }
}
