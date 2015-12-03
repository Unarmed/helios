/*
 * Copyright (c) 2014 Spotify AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

// TODO(negz): Dedupe with com.spotify.helios.servicescommon, move into docker-client?
package com.spotify.helios.testing;

import com.google.common.net.HostAndPort;

import java.net.URI;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.System.getenv;

/**
 * Represents a dockerd endpoint. A codified DOCKER_HOST.
 */
public class DockerHost {

  public static final int DEFAULT_PORT = 2375;
  public static final String DEFAULT_HOST = "localhost";
  public static final String DEFAULT_UNIX_ENDPOINT = "unix:///var/run/docker.sock";

  private final String host;
  private final URI uri;
  private final URI bindURI;
  private final String address;
  private final int port;
  private final String dockerCertPath;

  private DockerHost(final String endpoint, final String dockerCertPath) {
    if (endpoint.startsWith("unix://")) {
      this.port = 0;
      this.address = DEFAULT_HOST;
      this.host = endpoint;
      this.uri = URI.create(endpoint);
      this.bindURI = URI.create(endpoint);
    } else {
      final String stripped = endpoint.replaceAll(".*://", "");
      final HostAndPort hostAndPort = HostAndPort.fromString(stripped);
      final String hostText = hostAndPort.getHostText();
      final String scheme = isNullOrEmpty(dockerCertPath) ? "http" : "https";

      this.port = hostAndPort.getPortOrDefault(defaultPort());
      this.address = isNullOrEmpty(hostText) ? DEFAULT_HOST : hostText;
      this.host = address + ":" + port;
      this.uri = URI.create(scheme + "://" + address + ":" + port);
      this.bindURI = URI.create("tcp://" + address + ":" + port);
    }

    this.dockerCertPath = dockerCertPath;
  }

  /**
   * Get a docker endpoint usable for instantiating a new DockerHost with DockerHost.from(endpoint).
   *
   * @return The hostname.
   */
  public String host() {
    return host;
  }

  /**
   * Get the docker rest uri.
   *
   * @return The uri of the host.
   */
  public URI uri() {
    return uri;
  }

  /**
   * Get the docker rest bind uri.
   *
   * @return The uri of the host for binding ports (or setting $DOCKER_HOST).
   */
  public URI bindURI() {
    return bindURI;
  }
  /**
   * Get the docker endpoint port.
   *
   * @return The port.
   */
  public int port() {
    return port;
  }

  /**
   * Get the docker ip address or hostname.
   *
   * @return The ip address or hostname.
   */
  public String address() {
    return address;
  }

  /**
   * Get the path to certificate and key for connecting to Docker via HTTPS.
   *
   * @return The path to the certificate.
   */
  public String dockerCertPath() {
    return dockerCertPath;
  }

  /**
   * Create a {@link DockerHost} from DOCKER_HOST and DOCKER_PORT env vars.
   *
   * @return The DockerHost object.
   */
  public static DockerHost fromEnv() {
    String defaultEndpoint;
    if (System.getProperty("os.name").toLowerCase().equals("linux")) {
      defaultEndpoint = DEFAULT_UNIX_ENDPOINT;
    } else {
      defaultEndpoint = DEFAULT_HOST + ":" + defaultPort();
    }

    final String host = fromNullable(getenv("DOCKER_HOST")).or(defaultEndpoint);
    final String dockerCertPath = getenv("DOCKER_CERT_PATH");

    return new DockerHost(host, dockerCertPath);
  }

  /**
   * Create a {@link DockerHost} from an explicit address or uri.
   *
   * @param endpoint The docker endpoint.
   * @param dockerCertPath The certificate path.
   * @return The DockerHost object.
   */
  public static DockerHost from(final String endpoint, final String dockerCertPath) {
    return new DockerHost(endpoint, dockerCertPath);
  }

  private static int defaultPort() {
    final String port = getenv("DOCKER_PORT");
    if (port == null) {
      return DEFAULT_PORT;
    }
    try {
      return Integer.valueOf(port);
    } catch (NumberFormatException e) {
      return DEFAULT_PORT;
    }
  }

  @Override
  public String toString() {
    return host();
  }
}
