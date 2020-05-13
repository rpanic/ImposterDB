package observable

import aNewCollections.ChangeArgs
import aNewCollections.ElementChangeType
import aNewCollections.SetChangeArgs
import virtual.VirtualSet
import kotlin.reflect.KProperty

abstract class RelayingObject {
//    fun relay(args: ChangeArgs<*>, levels: LevelInformation)
    
    //Is a call from the child object to hook the parent object
    fun Any.hookTo(obj: Any){
        if(obj is Observable){
            if(this is VirtualSet<*>){
                //VirutalSet binds to Parent Observable
                val t = this as VirtualSet<Observable>
                obj.addListener { prop: KProperty<*>, old: Any, new: Any, levels: LevelInformation ->
                    t.tellChildren(SetChangeArgs<Observable>(ElementChangeType.Update, obj), levels.append(obj, old, new, prop))
                }
            }
        }else if(obj is VirtualSet<*>){
            if(this is Observable){
                //Observable binds to Parent VirtualSet
                obj.addListener { args, levels ->
                
                }
            }
        }
    }
    
    fun <T : Observable> VirtualSet<T>.hookTo(obj: T){
        obj.addListener { prop: KProperty<*>, old: Any, new: Any, levels: LevelInformation ->
            this.tellChildren(SetChangeArgs<T>(ElementChangeType.Update, obj), levels.append(obj, old, new, prop))
        }
    }
    
    fun <T : Observable> T.hookTo(set: VirtualSet<T>, prop: KProperty<T>){
        set.addListener { changeArgs, levelInformation ->
//            this.changed(prop, changeArgs.elements[0], changeArgs.elements[0], levelInformation.append(VirtualSetLevel(set, )))
        }
    }
    
}