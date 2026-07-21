package com.asc.fr.docspace.adapters.output.client.docspace;

import com.asc.fr.docspace.adapters.output.client.docspace.transfer.response.DocSpaceFileResponse;
import com.asc.fr.docspace.domain.docspace.DocSpaceRawFile;
import com.asc.fr.docspace.domain.docspace.DocSpaceUploadedFile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DocSpaceMapper {
  DocSpaceMapper INSTANCE = Mappers.getMapper(DocSpaceMapper.class);

  @Mapping(target = "fileId", source = "response.id")
  DocSpaceUploadedFile toUploadedFile(
      DocSpaceFileResponse response, String filename, String folderId);

  @Mapping(target = "fileName", source = "filename")
  @Mapping(target = "rawContent", source = "content")
  DocSpaceRawFile toDownloadedFile(String filename, byte[] content);
}
