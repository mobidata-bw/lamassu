package org.entur.lamassu.leader.entityupdater;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.entur.lamassu.cache.EntityCache;
import org.entur.lamassu.cache.StationSpatialIndex;
import org.entur.lamassu.delta.DeltaType;
import org.entur.lamassu.delta.GBFSEntityDelta;
import org.entur.lamassu.delta.GBFSFileDelta;
import org.entur.lamassu.mapper.entitymapper.RentalUrisMapper;
import org.entur.lamassu.mapper.entitymapper.StationMapper;
import org.entur.lamassu.mapper.entitymapper.StationMergeMapper;
import org.entur.lamassu.mapper.entitymapper.StationMergeMapperImpl;
import org.entur.lamassu.mapper.entitymapper.TranslationMapper;
import org.entur.lamassu.metrics.MetricsService;
import org.entur.lamassu.model.entities.FormFactor;
import org.entur.lamassu.model.entities.PropulsionType;
import org.entur.lamassu.model.entities.Station;
import org.entur.lamassu.model.entities.TranslatedString;
import org.entur.lamassu.model.entities.Translation;
import org.entur.lamassu.model.entities.VehicleType;
import org.entur.lamassu.model.entities.VehicleTypeAvailability;
import org.entur.lamassu.model.provider.FeedProvider;
import org.entur.lamassu.service.SpatialIndexIdGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mobilitydata.gbfs.v3_0.station_information.GBFSData;
import org.mobilitydata.gbfs.v3_0.station_information.GBFSName;
import org.mobilitydata.gbfs.v3_0.station_information.GBFSStation;
import org.mobilitydata.gbfs.v3_0.station_information.GBFSStationInformation;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StationsUpdaterTest {

  @Mock
  private EntityCache<Station> stationCache;

  @Mock
  private StationSpatialIndex spatialIndex;

  @Mock
  private MetricsService metricsService;

  @Mock
  private EntityCache<VehicleType> vehicleTypeCache;

  private StationMapper stationMapper;
  private StationMergeMapper stationMergeMapper;
  private SpatialIndexIdGeneratorService spatialIndexIdGeneratorService;
  private StationsUpdater stationsUpdater;

  @BeforeEach
  void setUp() {
    // Initialize real mappers
    TranslationMapper translationMapper = new TranslationMapper();
    RentalUrisMapper rentalUrisMapper = new RentalUrisMapper();
    stationMapper = new StationMapper(translationMapper, rentalUrisMapper);
    stationMergeMapper = new StationMergeMapperImpl();

    spatialIndexIdGeneratorService = new SpatialIndexIdGeneratorService(vehicleTypeCache);
    stationsUpdater =
      new StationsUpdater(
        stationCache,
        spatialIndex,
        stationMapper,
        stationMergeMapper,
        metricsService,
        spatialIndexIdGeneratorService
      );
    // Set default values for cache TTLs
    ReflectionTestUtils.setField(stationsUpdater, "stationEntityCacheMinimumTtl", 30);
    ReflectionTestUtils.setField(stationsUpdater, "stationEntityCacheMaximumTtl", 300);
  }

  @Test
  void shouldHandleStationUpdate() {
    // Given
    var feedProvider = new FeedProvider();
    feedProvider.setSystemId("test-system");
    feedProvider.setCodespace("test");
    feedProvider.setOperatorId("test-operator");
    feedProvider.setLanguage("en");

    var stationId = "station-1";
    var currentStation = new Station();
    currentStation.setId(stationId);
    currentStation.setLat(59.9);
    currentStation.setLon(10.7);
    var oldName = new TranslatedString();
    var oldTranslation = new Translation();
    oldTranslation.setLanguage("en");
    oldTranslation.setValue("Old Station Name");
    oldName.setTranslation(List.of(oldTranslation));
    currentStation.setName(oldName);

    var stationInfo = new GBFSStation();
    stationInfo.setStationId(stationId);
    stationInfo.setLat(59.9);
    stationInfo.setLon(10.7);
    var gbfsName = new GBFSName();
    gbfsName.setText("New Station Name");
    gbfsName.setLanguage("en");
    stationInfo.setName(List.of(gbfsName));

    var stationStatus = new org.mobilitydata.gbfs.v3_0.station_status.GBFSStation();
    stationStatus.setStationId(stationId);
    stationStatus.setNumDocksAvailable(10);
    stationStatus.setIsInstalled(true);
    stationStatus.setIsRenting(true);
    stationStatus.setIsReturning(true);
    stationStatus.setLastReported(new Date());

    var stationInformationFeed = new GBFSStationInformation();
    var data = new GBFSData();
    data.setStations(List.of(stationInfo));
    stationInformationFeed.setData(data);

    // Mock behavior
    when(stationCache.get(stationId)).thenReturn(currentStation);

    var delta = new GBFSFileDelta<org.mobilitydata.gbfs.v3_0.station_status.GBFSStation>(
      1000L,
      2000L,
      60L,
      "station_status",
      List.of(new GBFSEntityDelta<>(stationId, DeltaType.UPDATE, stationStatus))
    );

    // When
    stationsUpdater.addOrUpdateStations(feedProvider, delta, stationInformationFeed);

    // Then
    verify(spatialIndex).addAll(any());
    verify(stationCache).updateAll(any(), eq(60), eq(TimeUnit.SECONDS));
    verify(spatialIndex, never()).removeAll(anySet());
    verify(stationCache, never()).removeAll(anySet());
  }

  @Test
  void shouldHandleStationDelete() {
    // Given
    var feedProvider = new FeedProvider();
    feedProvider.setSystemId("test-system");
    feedProvider.setCodespace("test");
    feedProvider.setOperatorId("test-operator");
    feedProvider.setLanguage("en");

    var stationId = "station-1";
    var currentStation = new Station();
    currentStation.setId(stationId);
    currentStation.setLat(59.9);
    currentStation.setLon(10.7);

    // Mock behavior
    when(stationCache.get(stationId)).thenReturn(currentStation);

    var delta = new GBFSFileDelta<org.mobilitydata.gbfs.v3_0.station_status.GBFSStation>(
      1000L,
      2000L,
      60L,
      "station_status",
      List.of(new GBFSEntityDelta<>(stationId, DeltaType.DELETE, null))
    );

    // Calculate expected spatial index ID
    var spatialIndexId = spatialIndexIdGeneratorService.createStationIndexId(
      currentStation,
      feedProvider
    );

    // When
    stationsUpdater.addOrUpdateStations(feedProvider, delta, null);

    // Then
    verify(spatialIndex).removeAll(Set.of(spatialIndexId));
    verify(stationCache).removeAll(Set.of(stationId));
    verify(spatialIndex, never()).addAll(anyMap());
    verify(stationCache, never()).updateAll(anyMap(), anyInt(), any(TimeUnit.class));
  }

  @Test
  void shouldHandleStationCreate() {
    // Given
    var feedProvider = new FeedProvider();
    feedProvider.setSystemId("test-system");
    feedProvider.setCodespace("test");
    feedProvider.setOperatorId("test-operator");
    feedProvider.setLanguage("en");

    var stationId = "station-1";

    var stationInfo = new GBFSStation();
    stationInfo.setStationId(stationId);
    stationInfo.setName(
      List.of(new GBFSName().withLanguage("en").withText("New Station"))
    );
    stationInfo.setLat(59.9);
    stationInfo.setLon(10.7);

    var stationStatus = new org.mobilitydata.gbfs.v3_0.station_status.GBFSStation();
    stationStatus.setStationId(stationId);
    stationStatus.setNumDocksAvailable(10);
    stationStatus.setIsInstalled(true);
    stationStatus.setIsRenting(true);
    stationStatus.setIsReturning(true);
    stationStatus.setLastReported(new Date());

    var stationInformationFeed = new GBFSStationInformation();
    var data = new GBFSData();
    data.setStations(List.of(stationInfo));
    stationInformationFeed.setData(data);

    // Mock behavior
    when(stationCache.get(stationId)).thenReturn(null);

    var delta = new GBFSFileDelta<>(
      1000L,
      2000L,
      60L,
      "station_status",
      List.of(new GBFSEntityDelta<>(stationId, DeltaType.CREATE, stationStatus))
    );

    // When
    stationsUpdater.addOrUpdateStations(feedProvider, delta, stationInformationFeed);

    // Then
    verify(spatialIndex, never()).removeAll(anySet());
    verify(spatialIndex).addAll(any());
    verify(stationCache).updateAll(any(), eq(60), eq(TimeUnit.SECONDS));
    verify(stationCache, never()).removeAll(anySet());
  }

  @Test
  void shouldSkipUpdateWhenMissingStationInformation() {
    // Given
    var feedProvider = new FeedProvider();
    feedProvider.setSystemId("test-system");

    var stationId = "station-1";
    var currentStation = new Station();
    currentStation.setId(stationId);

    var stationStatus = new org.mobilitydata.gbfs.v3_0.station_status.GBFSStation();
    stationStatus.setStationId(stationId);
    stationStatus.setNumVehiclesAvailable(5);

    // Mock behavior
    when(stationCache.get(stationId)).thenReturn(currentStation);

    var delta = new GBFSFileDelta<>(
      1000L,
      2000L,
      60L,
      "station_status",
      List.of(new GBFSEntityDelta<>(stationId, DeltaType.UPDATE, stationStatus))
    );

    // When
    stationsUpdater.addOrUpdateStations(feedProvider, delta, null);

    // Then
    verify(spatialIndex, never()).addAll(anyMap());
    verify(spatialIndex, never()).removeAll(anySet());
    verify(stationCache, never()).updateAll(anyMap(), anyInt(), any(TimeUnit.class));
    verify(stationCache, never()).removeAll(anySet());
  }

  @Test
  void shouldHandleStationUpdateWithSpatialIndexChange() {
    // Given
    var feedProvider = new FeedProvider();
    feedProvider.setSystemId("test-system");
    feedProvider.setCodespace("test");
    feedProvider.setOperatorId("test-operator");
    feedProvider.setLanguage("en");

    var stationId = "station-1";
    var bikeTypeId = "bike";

    var bikeType = new VehicleType();
    bikeType.setId(bikeTypeId);
    bikeType.setFormFactor(FormFactor.BICYCLE);
    bikeType.setPropulsionType(PropulsionType.HUMAN);

    var currentStation = new Station();
    currentStation.setId(stationId);
    currentStation.setLat(59.9);
    currentStation.setLon(10.7);
    var oldAvailability = new VehicleTypeAvailability();
    oldAvailability.setVehicleTypeId(bikeTypeId);
    oldAvailability.setCount(5);
    currentStation.setVehicleTypesAvailable(List.of(oldAvailability));

    var stationInfo = new GBFSStation();
    stationInfo.setStationId(stationId);
    stationInfo.setName(
      List.of(new GBFSName().withLanguage("en").withText("Test Station"))
    );
    stationInfo.setLat(59.9);
    stationInfo.setLon(10.7);

    var stationStatus = new org.mobilitydata.gbfs.v3_0.station_status.GBFSStation();
    stationStatus.setStationId(stationId);
    // No vehicles available anymore
    stationStatus.setNumVehiclesAvailable(0);
    stationStatus.setNumDocksAvailable(10);
    stationStatus.setIsInstalled(true);
    stationStatus.setIsRenting(true);
    stationStatus.setIsReturning(true);
    stationStatus.setLastReported(new Date());

    var stationInformationFeed = new GBFSStationInformation();
    var data = new GBFSData();
    data.setStations(List.of(stationInfo));
    stationInformationFeed.setData(data);

    // Mock behavior
    when(stationCache.get(stationId)).thenReturn(currentStation);
    when(vehicleTypeCache.getAll(Set.of(bikeTypeId))).thenReturn(List.of(bikeType));

    var delta = new GBFSFileDelta<>(
      1000L,
      2000L,
      60L,
      "station_status",
      List.of(new GBFSEntityDelta<>(stationId, DeltaType.UPDATE, stationStatus))
    );

    // Calculate expected spatial index ID for verification
    var oldSpatialIndexId = spatialIndexIdGeneratorService.createStationIndexId(
      currentStation,
      feedProvider
    );

    // When
    stationsUpdater.addOrUpdateStations(feedProvider, delta, stationInformationFeed);

    // Then
    verify(spatialIndex).removeAll(Set.of(oldSpatialIndexId));
    verify(spatialIndex).addAll(any());
    verify(stationCache).updateAll(any(), eq(60), eq(TimeUnit.SECONDS));
    verify(stationCache, never()).removeAll(anySet());
  }
}
