package com.github.smallru8.plugins.audio;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import com.github.smallru8.NikoBot.event.Event.MessageEvent;
import com.github.smallru8.NikoBot.plugins.PluginsInterface;

public class Audio implements PluginsInterface{

	public static AudioController ac;
	public void onDisable() {
		// TODO Auto-generated method stub
		EventBus.getDefault().unregister(this);
	}

	public void onEnable() {
		// TODO Auto-generated method stub
		EventBus.getDefault().register(this);
		ac = new AudioController();
	}
	
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onMessageRecved(MessageEvent e) {
		if(!e.msg.getAuthor().isBot())
			ac.cmd(e.event);
	}
	
	public String pluginsName() {
		// TODO Auto-generated method stub
		return "Audio";
	}

}
