package services;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import models.HubsDescriptor;
import services.SignalJActor.Execute;
import services.SignalJActor.RegisterHub;
import services.SignalJActor.SendToChannel;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akkaGuice.PropsContext;

public class ChannelsActor extends UntypedActor {
	private final Map<String, ActorRef> channels = new HashMap<>();
	private final Map<String, HubsDescriptor.HubDescriptor> descriptors = new HashMap<>();

	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof RegisterHub) {
			final RegisterHub registerHub = (RegisterHub) message;
			final String channelName = registerHub.hub.getName();
			if(!channels.containsKey(channelName)) {
				ActorRef channel = getContext().actorOf(PropsContext.get(ChannelActor.class), channelName);
				channel.tell(registerHub, getSelf());
				channels.put(channelName, channel);
				descriptors.put(channelName, registerHub.descriptor);
			}
		}
		if(message instanceof ChannelJoin) {
			final ChannelJoin channelJoin = (ChannelJoin) message;
			for(final ActorRef channel : channels.values()) {
				channel.tell(channelJoin, getSelf());
			}
		}
		if(message instanceof SendToChannel) {
			final SendToChannel sendToChannel = (SendToChannel) message;
			final ActorRef channel = channels.get(sendToChannel.channel);
			channel.tell(new ChannelActor.Send(sendToChannel.message), getSelf());
		}
		if(message instanceof Execute) {
			final Execute execute = (Execute) message;
			final ActorRef channel = channels.get(execute.json.get("hub").textValue());
			channel.tell(execute, getSelf());
		}
	}
	
	public static class ChannelJoin {
		final UUID uuid;
		final ActorRef user; 
		
		public ChannelJoin(UUID uuid, ActorRef user) {
			this.uuid = uuid;
			this.user = user;
		}
	}
}