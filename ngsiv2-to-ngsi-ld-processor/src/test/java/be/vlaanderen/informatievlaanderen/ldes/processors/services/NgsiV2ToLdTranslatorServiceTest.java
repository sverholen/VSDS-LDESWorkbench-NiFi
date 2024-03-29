package be.vlaanderen.informatievlaanderen.ldes.processors.services;

import static be.vlaanderen.informatievlaanderen.ldes.processors.config.NgsiV2ToLdMapping.NGSI_V2_KEY_DATE_CREATED;
import static be.vlaanderen.informatievlaanderen.ldes.processors.config.NgsiV2ToLdMapping.NGSI_V2_KEY_DATE_MODIFIED;
import static be.vlaanderen.informatievlaanderen.ldes.processors.config.NgsiV2ToLdMapping.NGSI_V2_KEY_ID;
import static be.vlaanderen.informatievlaanderen.ldes.processors.config.NgsiV2ToLdMapping.NGSI_V2_KEY_TYPE;
import static be.vlaanderen.informatievlaanderen.ldes.processors.config.NgsiV2ToLdMapping.translateKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;

import be.vlaanderen.informatievlaanderen.ldes.processors.NgsiV2ToLdTranslatorDefaults;
import be.vlaanderen.informatievlaanderen.ldes.processors.exceptions.InvalidNgsiLdContextException;
import be.vlaanderen.informatievlaanderen.ldes.processors.valuobjects.LinkedDataModel;

@WireMockTest(httpPort = 10101)
class NgsiV2ToLdTranslatorServiceTest {

	private final String localCoreContext = "http://localhost:10101/ngsi-ld-core-context.json";
	private final String localLdContext = "http://localhost:10101/water-quality-observed-context.json";
	private final String remoteCoreContext = NgsiV2ToLdTranslatorDefaults.DEFAULT_CORE_CONTEXT;
	private final String remoteLdContext = NgsiV2ToLdTranslatorDefaults.TARGET_LD_CONTEXT;

	private final String idV2 = "waterqualityobserved:Sevilla:D1";
	private final String type = "WaterQualityObserved";

	private static final String DEVICE_V2 = "device_ngsiv2.json";
	private static final String DEVICE_LD = "device_ngsild.json";
	private static final String DEVICE_MODEL_V2 = "device_model_ngsiv2.json";
	private static final String DEVICE_MODEL_LD = "device_model_ngsild.json";
	private static final String WATER_QUALITY_OBSERVED_V2 = "water_quality_observed_ngsiv2.json";
	private static final String WATER_QUALITY_OBSERVED_LD = "water_quality_observed_ngsild.json";

	private NgsiV2ToLdTranslatorService translator;
	private final JsonObject data = new JsonObject();

	@BeforeEach
	void setup() {
		data.put(NGSI_V2_KEY_ID, idV2);
		data.put(NGSI_V2_KEY_TYPE, type);
	}

	@Test
	void whenIdFound_thenIdTranslated() {
		translator = new NgsiV2ToLdTranslatorService(localCoreContext, localLdContext);

		String idLd = "urn:ngsi-ld:WaterQualityObserved:" + idV2;
		JsonObject model = translator.translate(data.toString()).getModel();

		assertEquals(idLd, model.get(translateKey(NGSI_V2_KEY_ID)).getAsString().value(), "Translate ID");
	}

	@Test
	void whenTypeFound_thenTypeTranslated() {
		translator = new NgsiV2ToLdTranslatorService(localCoreContext, localLdContext);

		JsonObject model = translator.translate(data.toString()).getModel();

		assertEquals(type, model.get(translateKey(NGSI_V2_KEY_TYPE)).getAsString().value(), "Translate type");
	}

	@Test
	void whenDateFound_thenDateNormalised() {
		translator = new NgsiV2ToLdTranslatorService(localCoreContext, localLdContext);

		String expectedDate = "2017-01-31T06:45:00Z";
		JsonObject model;

		data.put(NGSI_V2_KEY_DATE_CREATED, "2017-01-31T06:45:00");
		model = translator.translate(data.toString()).getModel();

		assertEquals(expectedDate, model.get(translateKey(NGSI_V2_KEY_DATE_CREATED)).getAsString().value(),
				"Translate a date");

		data.put(NGSI_V2_KEY_DATE_CREATED, expectedDate);
		model = translator.translate(data.toString()).getModel();

		assertEquals(expectedDate, model.get(translateKey(NGSI_V2_KEY_DATE_CREATED)).getAsString().value(),
				"Translate a date");
	}

	@Test
	void whenDateCreatedFound_thenDateCreatedTranslated() {
		translator = new NgsiV2ToLdTranslatorService(localCoreContext, localLdContext);

		data.put(NGSI_V2_KEY_DATE_CREATED, "2017-01-31T06:45:00");
		JsonObject model = translator.translate(data.toString()).getModel();

		assertEquals("2017-01-31T06:45:00Z", model.get(translateKey(NGSI_V2_KEY_DATE_CREATED)).getAsString().value(),
				"Translate dateCreated");
	}

	@Test
	void whenDateModifiedFound_thenDateModifiedTranslated() {
		translator = new NgsiV2ToLdTranslatorService(localCoreContext, localLdContext);

		data.put(NGSI_V2_KEY_DATE_MODIFIED, "2017-01-31T06:45:00");
		JsonObject model = translator.translate(data.toString()).getModel();

		assertEquals("2017-01-31T06:45:00Z", model.get(translateKey(NGSI_V2_KEY_DATE_MODIFIED)).getAsString().value(),
				"Translate dateModified");
	}

	@Test
	void whenCoreContextIsNull_thenInvalidNgsiLdContextExceptionIsThrown() throws Exception {
		String coreContext = null;
		Path data = Paths.get(String.valueOf(getFile(DEVICE_V2)));

		translator = new NgsiV2ToLdTranslatorService(coreContext);

		String contents = Files.readString(data);

		assertThrows(InvalidNgsiLdContextException.class, () -> translator.translate(contents));
	}

	@Test
	void whenNgsiv2DataHasDateObserved_thenNgsiLdHasDateObserved() throws Exception {
		LinkedDataModel v2Model = getV2LinkedDataModel(WATER_QUALITY_OBSERVED_V2, true);

		assertTrue(v2Model.hasDateObserved());
	}

	@Test
	void whenDeviceNgsiIsInput_thenDeviceNgsiIsTranslatedWithLocalContext() throws Exception {
		testTranslationLocalContext(DEVICE_V2, DEVICE_LD, "Translate Device NGSIv2 (local context)");
	}

	@Test
	void whenDeviceNgsiIsInput_thenDeviceNgsiIsTranslatedWithRemoteContext() throws Exception {
		testTranslationRemoteContext(DEVICE_V2, DEVICE_LD, "Translate Device NGSIv2 (remote context)");
	}

	@Test
	void whenDeviceNgsiIsInputWithWKTTranslationTrue_thenDeviceNgsiIsTranslatedWithLocalContextAndWktTranslation()
			throws Exception {
		testTranslationLocalContext(DEVICE_V2, DEVICE_LD,
				"Translate Device NGSIv2 (local context), with geo:json -> wkt translation added");
	}

	@Test
	void whenDeviceModelNgsiIsInput_thenDeviceModelNgsiIsTranslatedLocalContext() throws Exception {
		testTranslationLocalContext(DEVICE_MODEL_V2, DEVICE_MODEL_LD, "Translate DeviceModel NGSIv2 (local context)");
	}

	@Test
	void whenDeviceModelNgsiIsInput_thenDeviceModelNgsiIsTranslatedWithRemoteContext() throws Exception {
		testTranslationRemoteContext(DEVICE_MODEL_V2, DEVICE_MODEL_LD, "Translate DeviceModel NGSIv2 (remote context)");
	}

	@Test
	void whenDeviceModelNgsiIsInputWithWKTTranslationTrue_thenDeviceModelNgsiIsTranslatedWithLocalContextAndWktTranslation()
			throws Exception {
		testTranslationLocalContext(DEVICE_MODEL_V2, DEVICE_MODEL_LD,
				"Translate DeviceModel NGSIv2 (local context), with geo:json -> wkt translation added");
	}

	@Test
	void whenWaterQualityObservedNgsiIsInput_thenWaterQualityObservedNgsiIsTranslatedWithLocalContext()
			throws Exception {
		testTranslationLocalContext(WATER_QUALITY_OBSERVED_V2, WATER_QUALITY_OBSERVED_LD,
				"Translate WaterQualityObserved NGSIv2 (local context)");
	}

	@Test
	void whenWaterQualityObservedNgsiIsInput_thenWaterQualityObservedNgsiIsTranslatedWithRemoteContext()
			throws Exception {
		testTranslationRemoteContext(WATER_QUALITY_OBSERVED_V2, WATER_QUALITY_OBSERVED_LD,
				"Translate WaterQualityObserved NGSIv2 (remote context)");
	}

	@Test
	void whenWaterQualityObservedNgsiIsInputWithWKTTranslationTrue_thenWaterQualityObservedNgsiIsTranslatedWithLocalContextAndWktTranslation()
			throws Exception {
		testTranslationLocalContext(WATER_QUALITY_OBSERVED_V2, WATER_QUALITY_OBSERVED_LD,
				"Translate WaterQualityObserved NGSIv2 (local context), with geo:json -> wkt translation added");
	}

	private void testTranslationLocalContext(String input, String expected, String message) throws Exception {
		testTranslation(true, input, expected, message);
	}

	private void testTranslationRemoteContext(String input, String expected, String message) throws Exception {
		testTranslation(false, input, expected, message);
	}

	private void testTranslation(boolean local, String input, String expected, String message) throws Exception {
		Model v2Model = getV2Model(input, local);
		Model ldModel = getLdModel(expected, local);

		assertTrue(ldModel.isIsomorphicWith(v2Model), message);
	}

	private Model getV2Model(String input, boolean local) throws Exception {
		return getV2LinkedDataModel(input, local).toRDFModel(Lang.JSONLD11);
	}

	private LinkedDataModel getV2LinkedDataModel(String input, boolean local) throws Exception {
		translator = new NgsiV2ToLdTranslatorService(local ? localCoreContext : remoteCoreContext,
				local ? localLdContext : remoteLdContext);

		Path v2 = Paths.get(String.valueOf(getFile(input)));

		return translator.translate(Files.readString(v2));
	}

	private Model getLdModel(String expected, boolean local) throws Exception {
		Path ld = Paths.get(String.valueOf(getFile((local ? "local" : "remote") + "_context/" + expected)));

		Model ldModel = ModelFactory.createDefaultModel();
		RDFParser.source(ld)
				.forceLang(Lang.JSONLD11)
				.parse(ldModel);

		return ldModel;
	}

	private File getFile(String input) throws Exception {
		return new File(Objects.requireNonNull(getClass().getClassLoader().getResource(input)).toURI());
	}
}
