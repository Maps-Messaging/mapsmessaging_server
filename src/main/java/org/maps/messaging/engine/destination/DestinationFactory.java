package org.maps.messaging.engine.destination;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.maps.messaging.api.features.DestinationType;
import org.maps.messaging.engine.destination.subscription.SubscriptionController;

public interface DestinationFactory {

  String getRoot();

  List<DestinationImpl> getDestinations();

  DestinationImpl find(String name);

  DestinationImpl findOrCreate(String name) throws IOException;

  DestinationImpl findOrCreate(String name, DestinationType destinationType) throws IOException;

  DestinationImpl create(@NotNull String name, @NotNull DestinationType destinationType) throws IOException;

  DestinationImpl delete(DestinationImpl destinationImpl);

  Map<String, DestinationImpl> get();

  void addListener(DestinationManagerListener subscriptionController);

  void removeListener(DestinationManagerListener subscriptionController);
}
