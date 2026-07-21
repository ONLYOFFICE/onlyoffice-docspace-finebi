package com.asc.fr.docspace.adapters.output.client.fr;

import com.asc.fr.docspace.adapters.output.client.fr.transfer.response.FineAttachmentResponse;
import com.asc.fr.docspace.adapters.output.client.fr.transfer.response.FineFolderNodeResponse;
import com.asc.fr.docspace.domain.fr.FineAttachment;
import com.asc.fr.docspace.domain.fr.FineFolder;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface FineMapper {
  FineMapper INSTANCE = Mappers.getMapper(FineMapper.class);

  default FineAttachment toAttachment(FineAttachmentResponse response) {
    if (response == null) return null;

    return new FineAttachment(response.getAttachId());
  }

  default FineFolder toFolder(FineFolderNodeResponse node) {
    return new FineFolder(node.getId(), node.getName());
  }
}
