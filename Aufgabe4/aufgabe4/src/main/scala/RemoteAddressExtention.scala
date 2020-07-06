import akka.actor.{ActorSystem, Address, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}

class RemoteAddressExtensionImpl(system: ExtendedActorSystem) extends Extension {
  def address: Address = system.provider.getDefaultAddress
}

object RemoteAddressExtension extends ExtensionId[RemoteAddressExtensionImpl] with ExtensionIdProvider {
  override def lookup: ExtensionId[_ <: Extension] = RemoteAddressExtension
  override def createExtension(system: ExtendedActorSystem) = new RemoteAddressExtensionImpl(system)
  override def get(system: ActorSystem): RemoteAddressExtensionImpl = super.get(system)
}
