/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 */

package io.mapsmessaging.network.io.impl.rdma;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;

final class RdmaNative implements AutoCloseable {

  private final Arena arena;
  private final SymbolLookup verbs;
  private final SymbolLookup rdmaCm;

  RdmaNative() throws IOException {
    arena = Arena.ofShared();
    try {
      verbs = SymbolLookup.libraryLookup("ibverbs", arena);
      rdmaCm = SymbolLookup.libraryLookup("rdmacm", arena);
      requireSymbol(verbs, "ibv_alloc_pd");
      requireSymbol(verbs, "ibv_reg_mr");
      requireSymbol(verbs, "ibv_create_cq");
      requireSymbol(verbs, "ibv_create_qp");
      requireSymbol(verbs, "ibv_post_send");
      requireSymbol(verbs, "ibv_poll_cq");
      requireSymbol(rdmaCm, "rdma_create_event_channel");
      requireSymbol(rdmaCm, "rdma_create_id");
      requireSymbol(rdmaCm, "rdma_resolve_addr");
      requireSymbol(rdmaCm, "rdma_resolve_route");
      requireSymbol(rdmaCm, "rdma_connect");
    } catch (RuntimeException | IOException e) {
      arena.close();
      if (e instanceof IOException ioException) {
        throw ioException;
      }
      throw new IOException("Unable to load rdma-core native libraries", e);
    }
  }

  SymbolLookup verbs() {
    return verbs;
  }

  SymbolLookup rdmaCm() {
    return rdmaCm;
  }

  @Override
  public void close() {
    arena.close();
  }

  private static void requireSymbol(SymbolLookup lookup, String symbol) throws IOException {
    if (lookup.find(symbol).isEmpty()) {
      throw new IOException("Required RDMA symbol not found: " + symbol);
    }
  }
}
