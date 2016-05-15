/**
 * Copyright (C) 2008-2010 Guenther Niess. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.frogx.service.core;


import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.frogx.service.api.MUGManager;
import org.frogx.service.api.MUGPersistenceProvider;
import org.frogx.service.api.MUGService;
import org.frogx.service.api.MultiUserGame;
import org.frogx.service.api.util.LocaleUtil;

import org.xmpp.component.Component;
import org.xmpp.component.ComponentException;
import org.xmpp.component.ComponentManager;
import org.xmpp.component.IQResultListener;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;

/**
 * A dummy implementation of  {@link MUGManager} , intended to be used during unit tests. This manager creates a  {@link DefaultMUGService}  which can be accessed via the  {@link #getMultiUserGamingService()}  method. Any  {@link Packet}  that should be processed by the gaming component can be applied by the  {@link #processPacket(Packet)}  method. Instances store any packets that are delivered to be send using the {@link #sendPacket(Packet)}   method or the  {@link ComponentManager} in a blocking queue. The content of this queue can be inspected using {@link #getSentPacket()} . Typically these queues are used to retrieve a response that was generated by the component.
 * @author  G&uuml;nther Nie&szlig;, guenther.niess@web.de
 */
public class DummyMUGManager implements MUGManager {
	
	static public String xmppDomain = "example.com";
	static public String subDomain = "gaming";
	static public String description = "A gaming component for testing";
	
	private final BlockingQueue<Packet> queue = new LinkedBlockingQueue<Packet>();
	
	/**
	 * @uml.property  name="service"
	 * @uml.associationEnd  
	 */
	private MUGService service = null;
	/**
	 * @uml.property  name="locale"
	 * @uml.associationEnd  
	 */
	private LocaleUtil locale = null;
	private Map<String, MultiUserGame> games = new ConcurrentHashMap<String, MultiUserGame>();
	
	
	public DummyMUGManager() {
		this(null);
	}
	
	public DummyMUGManager(MUGPersistenceProvider storage) {
		if (storage == null) {
			storage = new DummyPersistenceProvider();
		}
		locale = new LocaleUtil() {
				public String getLocalizedString(String key) {
					return "Localized string for key: " + key;
				}
			};
		service = new DefaultMUGService(subDomain, description, this, games, storage);
		try {
			service.initialize(new JID(null, subDomain + "." + xmppDomain, null), new DummyComponentManager());
			service.start();
		}
		catch (ComponentException e) {
			throw new IllegalStateException("Can't initialize MUG service.", e);
		}
	}
	
	public void destroy() {
		for (String namespace : games.keySet()) {
			unregisterMultiUserGame(namespace);
		}
		service.shutdown();
		service = null;
		games.clear();
		queue.clear();
		locale = null;
	}
	
	public LocaleUtil getLocaleUtil() {
		return locale;
	}
	
	public String getServerName() {
		return xmppDomain;
	}
	
	public MUGService getMultiUserGamingService() {
		return service;
	}
	
	public boolean isGameRegistered(String namespace) {
		return (namespace != null && games.containsKey(namespace));
	}
	
	public void registerMultiUserGame(String namespace, MultiUserGame game) {
		service.registerMultiUserGame(namespace, game);
		games.put(namespace, game);
	}
	
	public void processPacket(Packet packet) {
		service.processPacket(packet);
	}
	
	public void sendPacket(MUGService mugService, Packet packet)
			throws ComponentException {
		queue.add(packet);
	}
	
	public Packet getSentPacket() throws InterruptedException {
		return queue.poll(2, TimeUnit.SECONDS);
	}
	
	public int getSentPacketQueueSize() {
		return queue.size();
	}
	
	public void unregisterMultiUserGame(String namespace) {
		service.unregisterMultiUserGame(namespace);
		games.remove(namespace);
	}
	
	
	public class DummyComponentManager implements ComponentManager {
		
		public void addComponent(String subdomain, Component component)
				throws ComponentException {
			throw new ComponentException("Feature not implemented!");
		}
		
		public String getProperty(String name) {
			return System.getProperty(name);
		}
		
		public String getServerName() {
			return xmppDomain;
		}
		
		public boolean isExternalMode() {
			return false;
		}
		
		public IQ query(Component component, IQ packet, long timeout)
				throws ComponentException {
			throw new ComponentException("Feature not implemented!");
		}
		
		public void removeComponent(String subdomain)
				throws ComponentException {
			throw new ComponentException("Feature not implemented!");
		}
		
		public void sendPacket(Component component, Packet packet)
				throws ComponentException {
			queue.add(packet);
		}
		
		public void setProperty(String name, String value) {
			System.setProperty(name, value);
		}
		
		public void query(Component component, IQ packet,
				IQResultListener listener) throws ComponentException {
			throw new ComponentException("Feature not implemented!");
		}
	}
}