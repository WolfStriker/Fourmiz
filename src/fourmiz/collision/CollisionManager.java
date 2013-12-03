/*//////////////////////////////////////////////////////////////////////
	This file is part of Bomberton, an Bomberman-like.
	Copyright (C) 2012-2013  Nicolas Barranger <wicowyn@gmail.com>

    Bomberton is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Bomberton is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Bomberton.  If not, see <http://www.gnu.org/licenses/>.
*///////////////////////////////////////////////////////////////////////

package fourmiz.collision;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fourmiz.engine.Abillity;
import fourmiz.engine.EntityListener;


public class CollisionManager implements CollidableListener, EntityListener{
	private Map<Integer, Set<TouchHandle>> colliHandle=new HashMap<Integer, Set<TouchHandle>>();
	private Map<Integer, Set<TouchMarker>> colliMarker=new HashMap<Integer, Set<TouchMarker>>();
	
	
	public void addEntity(Entity entity){
		for(Abillity abillity : entity.getAllAbillity()){
			abillityAdded(abillity);
		}
		
		entity.addEntityListener(this);
	}
	
	public void addEntity(Collection<Entity> collection){
		for(Entity entity : collection) addEntity(entity);
	}
	
	public void removeEntity(Entity entity){
		for(Abillity abillity : entity.getAllAbillity()){
			abillityRemoved(abillity);
		}
		
		entity.removeEntityListener(this);
	}
	
	public void removeEntity(Collection<Entity> collection){
		for(Entity entity : collection) removeEntity(entity);
	}
	
	public void performCollision(){
		List<DataCollide> toCollides=new ArrayList<DataCollide>();
		
		for(Iterator<Map.Entry<Integer, Set<TouchHandle>>> itH=this.colliHandle.entrySet().iterator(); itH.hasNext();){
			Map.Entry<Integer, Set<TouchHandle>> entry=itH.next();
			Set<TouchMarker> setM=this.colliMarker.get(entry.getKey());
			
			if(setM==null) continue;
			
			for(TouchHandle tH : entry.getValue()){
				for(TouchMarker  tM : setM){
					if(tH.getArea().intersects(tM.getShape()) && tH!=tM){
						//TODO à optimiser, potiellement 2 même collision peuvent être prise en compte. Mais ne pose pas de problème.
						toCollides.add(new DataCollide(tH, tM));
					}
				}
			}
		}
		
		Collections.sort(toCollides);
		for(DataCollide data : toCollides) data.perform();		
	}

	@Override
	public void touchHandleAdded(TouchHandle handle) {
		Set<TouchHandle> set=colliHandle.get(handle.getType());
		
		if(set==null){
			set=new HashSet<TouchHandle>();
			CollisionManager.this.colliHandle.put(handle.getType(), set);
		}
		
		set.add(handle);
	}
	
	@Override
	public void touchHandleRemoved(TouchHandle handle) {
		Set<TouchHandle> set=colliHandle.get(handle.getType());
		
		set.remove(handle);
		if(set.isEmpty()) colliHandle.remove(handle.getType());
		
	}
	
	@Override
	public void touchMarkerAdded(TouchMarker marker) {
		Set<TouchMarker> set=colliMarker.get(marker.getType());
		
		if(set==null){
			set=new HashSet<TouchMarker>();
			CollisionManager.this.colliMarker.put(marker.getType(), set);
		}
		
		set.add(marker);
	}
	
	@Override
	public void touchMarkerRemoved(TouchMarker marker) {
		Set<TouchMarker> set=colliMarker.get(marker.getType());
		
		set.remove(marker);
		if(set.isEmpty()) CollisionManager.this.colliMarker.remove(marker.getType());
	}
	
	public class DataCollide implements Comparable<DataCollide>{
		public TouchHandle handle;
		public TouchMarker marker;
		
		
		public DataCollide(TouchHandle handle, TouchMarker marker){
			this.handle=handle;
			this.marker=marker;
		}
		
		@Override
		public int compareTo(DataCollide o) {
			return handle.compareTo(o.handle);
		}
		
		public void perform(){
			this.handle.perform(this.marker);
		}
		
	}

	@Override
	public void abillityAdded(Abillity abillity) {
		for(TouchHandle handle : abillity.getTouchHandle()) touchHandleAdded(handle);
		for(TouchMarker marker : abillity.getTouchMarker()) touchMarkerAdded(marker);
		
		abillity.addCollidableListener(this);
	}

	@Override
	public void abillityRemoved(Abillity abillity) {
		for(TouchHandle handle : abillity.getTouchHandle()) touchHandleRemoved(handle);
		for(TouchMarker marker : abillity.getTouchMarker()) touchMarkerRemoved(marker);
		
		abillity.removeCollidableListener(this);
	}
}
