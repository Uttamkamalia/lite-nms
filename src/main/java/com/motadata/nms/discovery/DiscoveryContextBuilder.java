package com.motadata.nms.discovery;


import com.motadata.nms.datastore.dao.CredentialProfileDAO;
import com.motadata.nms.models.DiscoveryProfile;
import com.motadata.nms.commons.NMSException;
import com.motadata.nms.commons.IPResolver;
import io.vertx.core.Future;

import java.util.List;

public class DiscoveryContextBuilder {

  private final CredentialProfileDAO credentialProfileDAO;

  public DiscoveryContextBuilder(CredentialProfileDAO credentialProfileDAO) {
    this.credentialProfileDAO = credentialProfileDAO;
  }

  public Future<DiscoveryContext> build(DiscoveryProfile profile) {
    List<String> resolvedIps;
    try {
      resolvedIps = IPResolver.resolveTargetIps(profile.target());
    } catch (IllegalArgumentException e) {
      return Future.failedFuture(NMSException.badRequest("Invalid target IP format", e));
    }

    // 2. Validate CredentialProfile exists
    return credentialProfileDAO.get(profile.credentialsProfileId())
      .compose(cp -> {
        if (cp == null) {
          return Future.failedFuture(NMSException.notFound("Credential Profile not found"));
        }

        // 3. Build DiscoveryContext
        DiscoveryContext context = new DiscoveryContext(
          resolvedIps,
          profile.port(),
          profile.credentialsProfileId(),
          profile.id()
        );
        return Future.succeededFuture(context);
      });
  }
}
