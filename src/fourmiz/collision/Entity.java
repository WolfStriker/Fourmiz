/*//////////////////////////////////////////////////////////////////////
	This file is part of Fourmiz, an simulation of ant live.
	Copyright (C) 2013  Nicolas Barranger <wicowyn@gmail.com>
						Jean-Baptiste Le Henaff <jb.le.henaff@gmail.com>
						Antoine Fouque <antoine.fqe@gmail.com>
						Julien Camenen <jcamenen@gmail.Com>
    Fourmiz is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Fourmiz is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Fourmiz.  If not, see <http://www.gnu.org/licenses/>.
*///////////////////////////////////////////////////////////////////////

package fourmiz.collision;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.newdawn.slick.geom.Shape;
import org.newdawn.slick.geom.Transform;
import org.newdawn.slick.geom.Vector2f;

import fourmiz.engine.Abillity;
import fourmiz.engine.Engine;
import fourmiz.engine.EntityListener;

/**
 * Classe Entity
 * D�finition d'une entit�e
 */
public final class Entity{
	private static Logger log=LogManager.getLogger(Entity.class);
	private static int lastID;
	private int ID;
	private float scale;
	private float direction;
	private Vector2f position;
	private boolean inUpdate=false;
	private boolean modifNCS=true; //NCS NormalCollisionShape, usefull for optimisation
	private List<EntityListener> entityListeners=new ArrayList<EntityListener>();
	private List<Abillity> abillities=new ArrayList<Abillity>();
	private Set<Abillity> abillitiesAdd=new HashSet<Abillity>();
	private Set<Abillity> abillitiesRemove=new HashSet<Abillity>();
	private Entity owner=null;
	private Shape normalCollisionShape=null;
	private Shape collisionShape=null;
	private Engine engine=null;
	
	{
		this.ID=Entity.lastID++;
	}
	
	
	public Entity(Engine engine, Shape collisionShape){
		this.normalCollisionShape=collisionShape;
		this.engine=engine;
	}
	
	public Engine getEngine(){
		return this.engine;
	}
	
	public void clear(){
		for(Abillity abillity : this.abillities) notifyAbillityRemoved(abillity);
		this.abillities.clear();
	}
	
	public void update(int delta){
		inUpdate=true;
		for(Abillity abillity : this.abillities) abillity.update(delta);
		inUpdate=false;
		checkAbillityBuff();
	}
	
	private void checkAbillityBuff(){
		for(Abillity abillity : this.abillitiesAdd) addAbillity(abillity);
		for(Abillity abillity : this.abillitiesRemove) removeAbillity(abillity);
		
		this.abillitiesAdd.clear();
		this.abillitiesRemove.clear();
	}
	
	public void addAbillity(Abillity abillity){
		if(this.inUpdate){
			this.abillitiesAdd.add(abillity);
			return;
		}
		
		abillity.setOwner(this);		
		this.abillities.add(abillity);
		
		notifyAbillityAdded(abillity);
		
		log.debug("abillity: "+abillity.getClass().getSimpleName()+" add to "+getID());
	}
	
	public boolean removeAbillity(Abillity abillity){
		if(this.inUpdate){
			if(!this.abillities.contains(abillity)) return false;
			return this.abillitiesRemove.add(abillity);
		}
		
		if(this.abillities.remove(abillity)){
			notifyAbillityRemoved(abillity);
			log.debug("abillity: "+abillity.getClass().getSimpleName()+" remove from "+getID());
			return true;
		}
		
		return false;
	}
	
	public List<Abillity> getAllAbillity(){
		return new ArrayList<Abillity>(this.abillities);
	}
	
	public Abillity getAbillity(int ID){
		for(Abillity abillity : this.abillities){
			if(abillity.getID()==ID) return abillity;
		}
		
		return null;
	}
	
	public static Entity getRootOwner(Entity entity){
		while(entity.getOwner()!=null){
			entity=entity.getOwner();
		}
			
		return entity;
	}

	public int getID(){
		return ID;
	}

	public float getScale(){
		return scale;
	}

	public void setScale(float scale){
		this.scale=scale;
	}

	public float getDirection(){
		return direction;
	}

	public void setDirection(float direction){
		this.direction=direction;
		this.modifNCS=true;
		
		while(this.direction<0) this.direction+=360;
		while(this.direction>=360) this.direction-=360;
		
		notifyPositionUpdated();
	}

	public Vector2f getPosition(){
		return this.position.copy();
	}

	public void setPosition(Vector2f position){
		this.position=position;
		this.modifNCS=true;
		
		notifyPositionUpdated();
	}
	
	public Entity getOwner(){
		return this.owner;
	}
	
	public void setOwner(Entity owner){
		this.owner=owner;
	}
	
	public boolean addEntityListener(EntityListener listener){
		return this.entityListeners.add(listener);
	}
	
	public boolean removeEntityListener(EntityListener listener){
		return this.entityListeners.remove(listener);
	}
	
	private void notifyAbillityAdded(Abillity abillity){
		for(EntityListener listener : this.entityListeners) listener.abillityAdded(abillity);
	}
	
	private void notifyAbillityRemoved(Abillity abillity){
		for(EntityListener listener : this.entityListeners) listener.abillityRemoved(abillity);
	}

	private void notifyPositionUpdated(){
		for(EntityListener listener : this.entityListeners) listener.positionUpdated();
	}
	
	public Shape getNormalCollisionShape(){
		return this.normalCollisionShape;
	}
	
	public Shape getCollisionShape(){
		if(modifNCS){
			collisionShape=normalCollisionShape.transform(Transform.createRotateTransform(
					(float) Math.toRadians(getDirection()), Engine.SIZE_CASE/2, Engine.SIZE_CASE/2))
					.transform(Transform.createTranslateTransform(position.x, position.y));
			
			modifNCS=false;
		}
		return collisionShape;
	}

	public boolean isCollidingWith(Entity collidable){
		return getCollisionShape().intersects(collidable.getCollisionShape());
	}
	
}
