package com.asc.fr.docspace.application.port.output;

import java.io.IOException;
import java.util.Optional;

public interface IUnitOfWork {
  interface Context {
    <D> D getDAO(Class<D> daoClass);
  }

  @FunctionalInterface
  interface Work<T> {
    T execute(Context ctx) throws Exception;
  }

  @FunctionalInterface
  interface VoidWork {
    void execute(Context ctx) throws Exception;
  }

  <T> Optional<T> query(Work<T> work);

  <T> T write(Work<T> work) throws IOException;

  void write(VoidWork work) throws IOException;
}
