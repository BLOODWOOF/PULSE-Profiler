package com.bloodwolf.pulse.report;

import com.bloodwolf.pulse.Pulse;
import com.bloodwolf.pulse.config.PulseConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.zip.GZIPOutputStream;

public final class ReportIO {
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
	private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

	public record Result(Path file, String viewerUrl, String error) {}

	private ReportIO() {}

	public static Result saveAndUpload(JsonObject report) {
		String json = ReportBuilder.toJson(report);
		Path file = Pulse.config().saveLocal ? writeLocal(json, report.get("kind").getAsString()) : null;
		if (!Pulse.config().autoUpload) {
			return new Result(file, null, file == null ? "saving and upload are both disabled" : null);
		}
		try {
			return new Result(file, upload(json), null);
		} catch (Exception e) {
			Pulse.LOG.warn("Pulse upload failed", e);
			return new Result(file, null, e.getMessage());
		}
	}

	private static Path writeLocal(String json, String kind) {
		String name = "pulse-" + kind + "-" + FILE_TIME.format(Instant.now()) + ".pulse.json.gz";
		Path path = PulseConfig.reportsDir().resolve(name);
		try {
			Files.write(path, gzip(json));
		} catch (Exception e) {
			Pulse.LOG.warn("Could not write local report", e);
		}
		return path;
	}

	private static String upload(String json) throws Exception {
		String uploadUrl = Pulse.config().uploadUrl;
		HttpRequest request = HttpRequest.newBuilder(URI.create(uploadUrl))
			.timeout(Duration.ofSeconds(30))
			.header("Content-Type", "application/json")
			.header("Content-Encoding", "gzip")
			.POST(HttpRequest.BodyPublishers.ofByteArray(gzip(json)))
			.build();
		HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() / 100 != 2) {
			throw new IllegalStateException("upload HTTP " + response.statusCode() + " " + response.body());
		}
		JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
		String id = body.get("id").getAsString();
		String base = Pulse.config().viewerBaseUrl.replaceAll("/$", "");
		if (base.contains("#")) {
			return base + "/r/" + id;
		}
		return base + "/#/r/" + id;
	}

	private static byte[] gzip(String json) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
			gzip.write(json.getBytes(StandardCharsets.UTF_8));
		}
		return out.toByteArray();
	}
}
