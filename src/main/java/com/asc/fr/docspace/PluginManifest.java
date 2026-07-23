package com.asc.fr.docspace;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Single source of truth for plugin-wide constants shared between the Java plugin and the Preact
 * build. Loaded once from {@code /manifest.json} on the classpath; fails fast if the file is
 * missing or malformed.
 */
public final class PluginManifest {
  private static final PluginManifest INSTANCE = load();

  public final String pluginId;
  public final String sdkVersion;
  public final boolean debug;
  public final Module module;
  public final Assets assets;
  public final Aliases aliases;
  public final Events events;
  public final Endpoints endpoints;
  public final Limits limits;
  public final Schedulers schedulers;

  @JsonCreator
  public PluginManifest(
      @JsonProperty("pluginId") String pluginId,
      @JsonProperty("sdkVersion") String sdkVersion,
      @JsonProperty("debug") boolean debug,
      @JsonProperty("module") Module module,
      @JsonProperty("assets") Assets assets,
      @JsonProperty("aliases") Aliases aliases,
      @JsonProperty("events") Events events,
      @JsonProperty("endpoints") Endpoints endpoints,
      @JsonProperty("limits") Limits limits,
      @JsonProperty("schedulers") Schedulers schedulers) {
    this.pluginId = pluginId;
    this.sdkVersion = sdkVersion;
    this.debug = debug;
    this.module = module;
    this.assets = assets;
    this.aliases = aliases;
    this.events = events;
    this.endpoints = endpoints;
    this.limits = limits;
    this.schedulers = schedulers;
  }

  public static PluginManifest get() {
    return INSTANCE;
  }

  private static PluginManifest load() {
    try (InputStream in = PluginManifest.class.getResourceAsStream("/manifest.json")) {
      if (in == null) {
        throw new IllegalStateException("manifest.json not found on classpath");
      }
      return new ObjectMapper().readValue(in, PluginManifest.class);
    } catch (IOException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  public static final class Module {
    public final String id;
    public final String value;
    public final String displayName;
    public final int sortIndex;

    @JsonCreator
    public Module(
        @JsonProperty("id") String id,
        @JsonProperty("value") String value,
        @JsonProperty("displayName") String displayName,
        @JsonProperty("sortIndex") int sortIndex) {
      this.id = id;
      this.value = value;
      this.displayName = displayName;
      this.sortIndex = sortIndex;
    }
  }

  public static final class Assets {
    public final Asset navigation;

    @JsonCreator
    public Assets(@JsonProperty("navigation") Asset navigation) {
      this.navigation = navigation;
    }
  }

  public static final class Asset {
    public final String script;
    public final String style;

    @JsonCreator
    public Asset(@JsonProperty("script") String script, @JsonProperty("style") String style) {
      this.script = script;
      this.style = style;
    }
  }

  public static final class Events {
    @JsonProperty public Common common;
    @JsonProperty public Backend backend;
    @JsonProperty public Frontend frontend;

    public static final class Common {}

    public static final class Backend {
      @JsonProperty public String tenantReset;
      @JsonProperty public String datasetUpdated;
    }

    public static final class Frontend {
      @JsonProperty public String filePicker;
    }
  }

  /** Payload size caps in bytes. */
  public static final class Limits {
    /** JSON command bodies */
    @JsonProperty public int jsonBodyBytes;

    /** DocSpace webhook deliveries */
    @JsonProperty public int webhookBodyBytes;

    /** Raw XLSX bodies POSTed to the export upload endpoint. */
    @JsonProperty public int exportUploadBytes;

    /** Files uploaded to DocSpace, also caps FineBI export downloads */
    @JsonProperty public int docSpaceUploadBytes;

    /** Spreadsheets pulled from DocSpace for import. */
    @JsonProperty public int docSpaceDownloadBytes;
  }

  public static final class Schedulers {
    @JsonProperty public Schedule webhookReconciliation;

    @JsonProperty public SyncLinkSchedule syncLinkReconciliation;
  }

  public static class Schedule {
    @JsonProperty public long initialDelayMillis;

    @JsonProperty public long periodMillis;
  }

  public static final class SyncLinkSchedule extends Schedule {
    @JsonProperty public long staleAfterMillis;

    @JsonProperty public int maxPerRun;
  }

  /** All plugin-private HTTP handler paths, keyed by logical name. */
  public static final class Endpoints {
    @JsonProperty public String session;
    @JsonProperty public String setup;
    @JsonProperty public String login;
    @JsonProperty public String logout;
    @JsonProperty public String reset;
    @JsonProperty public String docspace;
    @JsonProperty public String docspaceAdmin;
    @JsonProperty public String pluginDebug;
    @JsonProperty public String importFile;
    @JsonProperty public String folders;
    @JsonProperty public String webhookCallback;
    @JsonProperty public String webhookRegister;
    @JsonProperty public String syncEvents;
    @JsonProperty public String exportConfig;
    @JsonProperty public String exportUpload;
    @JsonProperty public String exportTrigger;
  }

  public static final class Aliases {
    @JsonProperty public Alias main;
    @JsonProperty public Alias admin;
    @JsonProperty public Alias debug;
    @JsonProperty public Alias webhook;
    @JsonProperty public Alias events;

    public List<Alias> all() {
      return Arrays.asList(main, admin, debug, webhook, events);
    }
  }

  public static final class Alias {
    public final String from;
    public final String to;
    public final boolean isPublic;

    @JsonCreator
    public Alias(
        @JsonProperty("from") String from,
        @JsonProperty("to") String to,
        @JsonProperty("public") boolean isPublic) {
      this.from = from;
      this.to = to;
      this.isPublic = isPublic;
    }
  }
}
