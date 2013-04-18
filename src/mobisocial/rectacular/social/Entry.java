package mobisocial.rectacular.social;

import java.util.Set;

import mobisocial.rectacular.model.MEntry.EntryType;

/**
 * Wrapper for entry fields
 */
public class Entry {
    /**
     * Type of the entry
     */
    public EntryType type;
    
    /**
     * Name of the entry
     */
    public String name;
    
    /**
     * true if I have this, false if I don't
     */
    public boolean owned;
    
    /**
     * All the people known to have/had this
     */
    public Set<String> owners;
    
    /**
     * Optional extra data (e.g. thumbnail)
     */
    public byte[] extra;
    
    /**
     * Optional string metadata
     */
    public String metadata;
}
