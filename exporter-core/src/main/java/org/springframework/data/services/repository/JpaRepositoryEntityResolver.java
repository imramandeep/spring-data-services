package org.springframework.data.services.repository;

import java.io.Serializable;
import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.services.Resolver;
import org.springframework.data.services.util.UriUtils;
import org.springframework.util.ClassUtils;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class JpaRepositoryEntityResolver implements Resolver<CrudRepository> {

  @Autowired
  private JpaRepositoryMetadata repositoryMetadata;
  @Autowired
  private ConversionService conversionService;
  private URI baseUri;

  public JpaRepositoryEntityResolver(URI baseUri) {
    this.baseUri = baseUri;
  }

  public JpaRepositoryEntityResolver(URI baseUri, ConversionService conversionService) {
    this.baseUri = baseUri;
    this.conversionService = conversionService;
  }

  public JpaRepositoryEntityResolver(URI baseUri, JpaRepositoryMetadata repositoryMetadata, ConversionService conversionService) {
    this.baseUri = baseUri;
    this.repositoryMetadata = repositoryMetadata;
    this.conversionService = conversionService;
  }

  @Override public boolean supports(URI uri, Object target) {
    if (null == target) {
      return false;
    }
    if (uri.isAbsolute() && target instanceof CrudRepository) {
      CrudRepository repository = (CrudRepository) target;
      List<URI> uris = UriUtils.explode(baseUri, uri);
      if (uris.size() > 1) {
        URI repoUri = uris.get(uris.size() - 2);
        return repoUri.getPath().equals(repositoryMetadata.repositoryNameFor(repository));
      }
    }
    if (target instanceof CrudRepository) {
      return (null != repositoryMetadata.entityInfoFor((CrudRepository) target));
    } else {
      return false;
    }
  }

  @SuppressWarnings({"unchecked"})
  @Override public Object resolve(URI uri, CrudRepository repository) {
    EntityInformation entityInfo = repositoryMetadata.entityInfoFor(repository);
    if (uri.isAbsolute()) {
      List<URI> uris = UriUtils.explode(baseUri, uri);
      if (uris.size() > 1) {
        uri = uris.get(uris.size() - 1);
      }
    }
    String sid = uri.getPath();
    Class<? extends Serializable> idType = entityInfo.getIdType();
    return repository.findOne(
        !ClassUtils.isAssignable(idType, String.class)
            ? conversionService.convert(sid, idType)
            : sid
    );
  }

}