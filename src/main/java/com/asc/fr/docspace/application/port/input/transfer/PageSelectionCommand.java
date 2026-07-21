package com.asc.fr.docspace.application.port.input.transfer;

import com.asc.fr.docspace.application.port.Command;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PageSelectionCommand implements Command {
  boolean launcher;
  boolean logout;
  boolean setup;
  boolean admin;
  boolean configured;
  boolean hasLogin;

  @Override
  public boolean valid() {
    return true;
  }
}
