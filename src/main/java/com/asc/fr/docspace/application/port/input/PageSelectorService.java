package com.asc.fr.docspace.application.port.input;

import com.asc.fr.docspace.application.port.input.transfer.Page;
import com.asc.fr.docspace.application.port.input.transfer.PageSelectionCommand;

public interface PageSelectorService {
  Page select(PageSelectionCommand request);
}
