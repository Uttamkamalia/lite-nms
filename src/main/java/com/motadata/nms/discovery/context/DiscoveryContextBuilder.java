package com.motadata.nms.discovery.context;

import com.motadata.nms.commons.IPResolver;
import com.motadata.nms.commons.NMSException;
import com.motadata.nms.commons.VertxProvider;
import com.motadata.nms.models.DiscoveryProfile;
import com.motadata.nms.models.credential.CredentialProfile;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;

import java.util.List;

import static com.motadata.nms.utils.EventBusChannels.*;

public class DiscoveryContextBuilder {

  private static final Logger logger = LoggerFactory.getLogger(DiscoveryContextBuilder.class);

  private final Vertx vertx;

  public DiscoveryContextBuilder() {
    this.vertx = VertxProvider.getVertx();
  }

  /**
   * Build a DiscoveryContext from a DiscoveryProfile ID
   * @param profileId The ID of the DiscoveryProfile to use
   * @return A Future containing the built DiscoveryContext
   */
  public Future<DiscoveryContext> buildFromProfileId(Integer profileId) {
    logger.info("Building discovery context for profile ID: " + profileId);

    if (profileId == null) {
      return Future.failedFuture(NMSException.badRequest("Discovery Profile ID cannot be null"));
    }

    return getDiscoveryProfile(profileId)
      .compose(profile -> build(profile));
  }

  /**
   * Get a DiscoveryProfile by ID using the event bus
   * @param profileId The ID of the profile to get
   * @return A Future containing the DiscoveryProfile
   */
  private Future<DiscoveryProfile> getDiscoveryProfile(Integer profileId) {
    Promise<DiscoveryProfile> promise = Promise.promise();

    vertx.eventBus().request(DISCOVERY_PROFILE_GET.name(), profileId, getDiscoveryProfileReply -> {
      if (getDiscoveryProfileReply.succeeded()) {
        try {
          JsonObject profileJson = (JsonObject) getDiscoveryProfileReply.result().body();
          if (profileJson == null) {
            promise.fail(NMSException.notFound("Discovery Profile not found"));
            return;
          }
          //TODO check json == null defined twice here
          DiscoveryProfile profile = DiscoveryProfile.fromJson(profileJson);
          promise.complete(profile);
        } catch (Exception e) {
          promise.fail(NMSException.badRequest("Invalid Discovery Profile data", (IllegalArgumentException) e));
        }
      } else {
        promise.fail(getDiscoveryProfileReply.cause());
      }
    });

    return promise.future();
  }

  /**
   * Get a CredentialProfile by ID using the event bus
   * @param profileId The ID of the profile to get
   * @return A Future containing the CredentialProfile
   */
  private Future<CredentialProfile> getCredentialProfile(Integer profileId) {
    Promise<CredentialProfile> promise = Promise.promise();

    vertx.eventBus().request(CREDENTIAL_PROFILE_GET.name(), profileId, reply -> {
      if (reply.succeeded()) {
        try {
          JsonObject profileJson = (JsonObject) reply.result().body();
          if (profileJson == null) {
            promise.fail(NMSException.notFound("Credential Profile not found"));
            return;
          }

          CredentialProfile profile = CredentialProfile.fromJson(profileJson);
          promise.complete(profile);
        } catch (Exception e) {
          promise.fail(NMSException.badRequest("Invalid Credential Profile data", (IllegalArgumentException) e));
        }
      } else {
        promise.fail(reply.cause());
      }
    });

    return promise.future();
  }

  /**
   * Build a DiscoveryContext from a DiscoveryProfile
   * @param profile The DiscoveryProfile to use
   * @return A Future containing the built DiscoveryContext
   */
  public Future<DiscoveryContext> build(DiscoveryProfile profile) {
    return validateProfile(profile)
      .compose(this::resolveTargetIps)
      .compose(this::fetchCredentialProfile)
      .compose(this::createDiscoveryContext);
  }

  /**
   * Validate the discovery profile
   * @param profile The profile to validate
   * @return A Future containing the validated profile
   */
  private Future<DiscoveryProfile> validateProfile(DiscoveryProfile profile) {
    if (profile == null) {
      return Future.failedFuture(NMSException.badRequest("Discovery Profile cannot be null"));
    }

    if (profile.getTarget() == null || profile.getTarget().isEmpty()) {
      return Future.failedFuture(NMSException.badRequest("Target IP cannot be empty"));
    }

    if (profile.getCredentialsProfileId() == null) {
      return Future.failedFuture(NMSException.badRequest("Credentials Profile ID cannot be null"));
    }

    return Future.succeededFuture(profile);
  }

  /**
   * Resolve target IPs from the profile
   * @param profile The profile containing the target
   * @return A Future containing a tuple of the profile and resolved IPs
   */
  private Future<ProfileWithIps> resolveTargetIps(DiscoveryProfile profile) {
    try {
      List<String> resolvedIps = IPResolver.resolveTargetIps(profile.getTarget());

      if (resolvedIps.isEmpty()) {
        return Future.failedFuture(NMSException.badRequest("No valid IPs could be resolved from target"));
      }

      if (!validateIps(resolvedIps)) {
        return Future.failedFuture(NMSException.badRequest("Invalid IP addresses in resolved list"));
      }

      return Future.succeededFuture(new ProfileWithIps(profile, resolvedIps));
    } catch (IllegalArgumentException e) {
      return Future.failedFuture(NMSException.badRequest("Invalid target IP format: " + e.getMessage(), e));
    }
  }

  /**
   * Fetch the credential profile for the discovery profile
   * @param profileWithIps The profile with resolved IPs
   * @return A Future containing a tuple of the profile, IPs, and credential profile
   */
  private Future<ProfileWithIpsAndCredential> fetchCredentialProfile(ProfileWithIps profileWithIps) {
    return getCredentialProfile(profileWithIps.profile.getCredentialsProfileId())
      .compose(credentialProfile -> {
        return Future.succeededFuture(new ProfileWithIpsAndCredential(
          profileWithIps.profile,
          profileWithIps.resolvedIps,
          credentialProfile
        ));
      });
  }

  /**
   * Create a DiscoveryContext from the profile, IPs, and credential profile
   * @param data The profile, IPs, and credential profile
   * @return A Future containing the built DiscoveryContext
   */
  private Future<DiscoveryContext> createDiscoveryContext(ProfileWithIpsAndCredential data) {
    Integer port = data.credentialProfile.getCredential().getType().getDefaultPort();

    DiscoveryContext context = new DiscoveryContext(
      data.resolvedIps,
      port,
      data.profile.getId(),
      data.credentialProfile
    );

    return Future.succeededFuture(context);
  }

  /**
   * Validate a list of IP addresses
   * @param ips The list of IP addresses to validate
   * @return True if all IPs are valid, false otherwise
   */
  private boolean validateIps(List<String> ips) {
    if (ips == null || ips.isEmpty()) {
      return false;
    }

    for (String ip : ips) {
      if (!IPResolver.isValidIp(ip)) {
        return false;
      }
    }

    return true;
  }

  // Helper classes to pass data between stages
  private static class ProfileWithIps {
    final DiscoveryProfile profile;
    final List<String> resolvedIps;

    ProfileWithIps(DiscoveryProfile profile, List<String> resolvedIps) {
      this.profile = profile;
      this.resolvedIps = resolvedIps;
    }
  }

  private static class ProfileWithIpsAndCredential {
    final DiscoveryProfile profile;
    final List<String> resolvedIps;
    final CredentialProfile credentialProfile;

    ProfileWithIpsAndCredential(DiscoveryProfile profile, List<String> resolvedIps, CredentialProfile credentialProfile) {
      this.profile = profile;
      this.resolvedIps = resolvedIps;
      this.credentialProfile = credentialProfile;
    }
  }
}
