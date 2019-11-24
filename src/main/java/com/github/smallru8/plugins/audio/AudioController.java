package com.github.smallru8.plugins.audio;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import com.github.smallru8.NikoBot.Embed;
import com.github.smallru8.plugins.audio.lavaPlayer.GuildMusicManager;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class AudioController {

	private final AudioPlayerManager playerManager;
	private final Map<Long, GuildMusicManager> musicManagers;
	private Message msg;
	
	public AudioController() {
		playerManager = new DefaultAudioPlayerManager();
		musicManagers = new HashMap<Long, GuildMusicManager>();
		AudioSourceManagers.registerRemoteSources(playerManager);
	    AudioSourceManagers.registerLocalSource(playerManager);
	}
	private synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {
	    long guildId = Long.parseLong(guild.getId());
	    GuildMusicManager musicManager = musicManagers.get(guildId);

	    if (musicManager == null) {
	    	musicManager = new GuildMusicManager(playerManager);
	    	musicManagers.put(guildId, musicManager);
	    }

	    guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

	    return musicManager;
	}
	public void cmd(GuildMessageReceivedEvent e) {
		this.msg = e.getMessage();
		String[] command = e.getMessage().getContentRaw().split(" ", 2);
	    Guild guild = e.getMessage().getGuild();
	    if (guild != null) {
	    	if (("/play".equals(command[0])||"/p".equals(command[0])) && command.length == 2) {
		        loadAndPlay(e.getChannel(), command[1]);
		    }else if ("/skip".equals(command[0])) {
		        skipTrack(e.getChannel());
		    }else if("/d".equals(command[0])) {//
		    	stopPlayer(e.getChannel());
		    	disconnectChannel();
		    }else if("/stop".equals(command[0])) {
		    	stopPlayer(e.getChannel());
		    }else if("/pause".equals(command[0])) {
		    	pausestartPlayer(e.getChannel(),true);
		    }else if("/play".equals(command[0])||"/p".equals(command[0])) {
		    	pausestartPlayer(e.getChannel(),false);
		    }else if("/r".equals(command[0])) {
		    	repeatPlay(e.getChannel());
		    }
	    }
	}
	private void loadAndPlay(final TextChannel channel, final String trackUrl) {
		final GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());

		playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
			public void trackLoaded(AudioTrack track) {
		        
		        Embed.EmbedSender(Color.pink, channel, ":regional_indicator_q:" + track.getInfo().title, "");
		        play(channel.getGuild(), musicManager, track);
		    }

		    public void playlistLoaded(AudioPlaylist playlist) {
		        AudioTrack firstTrack = playlist.getSelectedTrack();

		        if (firstTrack == null) {
		        	firstTrack = playlist.getTracks().get(0);
		        }

		        Embed.EmbedSender(Color.pink, channel, ":regional_indicator_q:"+ firstTrack.getInfo().title + " (first track of playlist " + playlist.getName() + ")", "");
		        
		        play(channel.getGuild(), musicManager, firstTrack);
		    }

		    public void noMatches() {
		    	Embed.EmbedSender(Color.pink, channel, ":exclamation:" + "Nothing found by " + trackUrl, "");
		    }

		    public void loadFailed(FriendlyException exception) {
		    	Embed.EmbedSender(Color.pink, channel, ":exclamation:" + "Could not play: " + exception.getMessage(), "");
		    }
		});
	}

	private void play(Guild guild, GuildMusicManager musicManager, AudioTrack track) {
		if(!guild.getAudioManager().isConnected() && !guild.getAudioManager().isAttemptingToConnect()) {
			VoiceChannel vc = msg.getMember().getVoiceState().getChannel();
			guild.getAudioManager().openAudioConnection(vc);
		}
		musicManager.scheduler.queue(track);
	}
	private void skipTrack(TextChannel channel) {
		GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
		musicManager.scheduler.nextTrack();
	
		Embed.EmbedSender(Color.pink, channel, ":track_next:Skipped to next track : "+musicManager.player.getPlayingTrack().getInfo().title, "");
		
	}
	private void stopPlayer(TextChannel channel) {
		repeatPlay(channel,false);
		GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
		musicManager.player.stopTrack();
	}
	private void pausestartPlayer(TextChannel channel,boolean b) {
		GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
		musicManager.player.setPaused(b);
		if(!b&&musicManager.player.getPlayingTrack()==null) {//stop >> play
			skipTrack(channel);
		}
	}
	private void repeatPlay(TextChannel channel) {
		GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
		String name = musicManager.player.getPlayingTrack().getInfo().title;
		if(musicManager.scheduler.repeatFlag) {
			musicManager.scheduler.repeatFlag = false;
			Embed.EmbedSender(Color.pink, channel, ":arrow_right:"+name, "");
		}else {
			musicManager.scheduler.repeatFlag = true;
			
			Embed.EmbedSender(Color.pink, channel, ":repeat:"+name, "");
		}
	}
	private void repeatPlay(TextChannel channel,boolean b) {
		GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
		if(b) {
			musicManager.scheduler.repeatFlag = true;
		}else {
			musicManager.scheduler.repeatFlag = false;
		}
	}
	private void disconnectChannel() {
		msg.getGuild().getAudioManager().closeAudioConnection();
	}
}
