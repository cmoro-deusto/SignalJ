package services;
import hubs.Hub;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import models.HubsDescriptor;
import play.Logger;
import play.mvc.WebSocket;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akkaGuice.PropsContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class SignalJActor extends UntypedActor  {
	private final ActorRef usersActor;
	private final ActorRef channelsActor;
	private final ActorRef hubsActor;
	private final Map<UUID, ActorRef> users = new HashMap<UUID, ActorRef>();
	
	@Inject
	public SignalJActor(@Named("HubsActor") ActorRef hubsActor) {
		this.usersActor = getContext().actorOf(PropsContext.get(UsersActor.class), "users");
		this.channelsActor = getContext().actorOf(PropsContext.get(ChannelsActor.class), "channels");
		this.hubsActor = hubsActor;
	}
	
	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof Join) {
			final Join join = (Join) message;
			final ActorRef user = getContext().actorOf(PropsContext.get(UserActor.class), join.uuid.toString());
			users.put(join.uuid, user);
			user.tell(join, getSelf());
			channelsActor.tell(new ChannelsActor.ChannelJoin(join.uuid, user), getSelf());
			Logger.debug(join.uuid + " logged on");
		}
		if(message instanceof Quit) {
			final Quit quit = (Quit) message;
			final ActorRef user = users.remove(quit.uuid);
			user.tell(new UserActor.Quit(), getSelf());
			Logger.debug(quit.uuid + " logged off");
		}
		if(message instanceof SendToAll) {
			final SendToAll sendToAll = (SendToAll) message;
			for(final ActorRef user : users.values()) {
				user.tell(new UserActor.Send(sendToAll.message), getSelf());
			}
		}
		if(message instanceof Send) {
			final Send send = (Send) message;
			final ActorRef user = users.get(send.uuid);
			user.tell(new UserActor.Send(send.message), getSelf());
		}
		if(message instanceof SendToChannel) {
			channelsActor.forward(message, getContext());
		}
		if(message instanceof RegisterHub) {
			channelsActor.forward(message, getContext());
		}
		if(message instanceof Execute) {
			channelsActor.forward(message, getContext());
		}
		if(message instanceof Describe) {
			hubsActor.forward(message, getContext());
		}
//		if(message instanceof ChannelJoin) {
//			final ChannelJoin channelJoin = (ChannelJoin) message;
//			final ActorRef user = users.get(channelJoin.uuid); 
//			channelsActor.tell(new ChannelsActor.ChannelJoin(channelJoin.channelName, channelJoin.uuid, user), getSelf());
//			
//			//channelsActor.tell(new ChannelsActor.ChannelJoin(channelJoin.channelName, channelJoin.uuid, user), sender);
////			Promise.wrap(ask(usersActor, new UsersActor.GetUser(channelJoin.uuid), 5000)).onRedeem(new Callback<Object>() {
////
////				@Override
////				public void invoke(Object arg0) throws Throwable {
////					ActorRef user = (ActorRef) arg0;
////					channelsActor.tell(new ChannelsActor.ChannelJoin(channelJoin.channelName, channelJoin.uuid, user), sender);
////				}
////			});
//			
//			
//		}
	}
	
	public static class Join {
		public final UUID uuid = UUID.randomUUID();
        final WebSocket.Out<JsonNode> out;
        final WebSocket.In<JsonNode> in;
        
        public Join(WebSocket.Out<JsonNode> out, WebSocket.In<JsonNode> in) {
            this.out = out;
            this.in = in;
        }
    }
	
	public static class ChannelJoin {
		final String channelName;
		final UUID uuid;
		
		public ChannelJoin(String channelName, UUID uuid) {
			this.channelName = channelName;
			this.uuid = uuid;
		}
	}
	
	public static class Quit {
		final UUID uuid;
		
		public Quit(UUID uuid) {
			this.uuid = uuid;
		}
	}
	
	public static class SendToAll {
		final String message;
		
		public SendToAll(String message) {
			this.message = message;
		}
	}
	
	public static class Send {
		final UUID uuid;
		final String message;
		
		public Send(UUID uuid, String message) {
			this.uuid = uuid;
			this.message = message; 
		}
	}
	
	public static class SendToChannel {
		final String channel;
		final String message;
		
		public SendToChannel(String channel, String message) {
			this.channel = channel;
			this.message = message;
		}
	}
	
	public static class RegisterHub {
		final Class<? extends Hub> hub;
		final HubsDescriptor.HubDescriptor descriptor;
		
		public RegisterHub(Class<? extends Hub> hub, HubsDescriptor.HubDescriptor descriptor) {
			this.hub = hub;
			this.descriptor = descriptor;
		}
	}
	
	public static class Execute {
		final JsonNode json;
		
		public Execute(JsonNode json) {
			this.json = json;
		}
	}
	
	public static class Describe {
		final JsonNode json;
		final ActorRef user;
		
		public Describe(JsonNode json, ActorRef user) {
			this.json = json;
			this.user = user;
		}
	}
}