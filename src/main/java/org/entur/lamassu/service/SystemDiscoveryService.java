package org.entur.lamassu.service;

import org.entur.gbfs.v3_0_RC2.manifest.GBFSManifest;
import org.entur.lamassu.model.discovery.SystemDiscovery;

public interface SystemDiscoveryService {
  SystemDiscovery getSystemDiscovery();
  GBFSManifest getGBFSManifest();
}
