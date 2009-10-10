package org.semispace.semimeter.gui;

import java.util.Date;
import java.util.List;

/**
 * A feed entry element
 */
public class GuiEntry {
    /* Example of entry to target
    <entry>
      <title>stale</title>
      <id>http://admin.example.org/events/1125</id>
      <updated>2007-04-13T10:31:01Z</updated>
      <link href="http://www.example.org/img/123.gif" type="image/gif"/>
      <link href="http://www.example.org/img/123.png" type="image/png"/>
      <cc:stale/>
    </entry>
    */

    // title is always stale
    private String id;
    private Date updated;
    private List<String> links;

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    /**
     * Utility method which returns time in ms.
     */
    public String getAge() {
        return String.valueOf( (System.currentTimeMillis() - updated.getTime()) / 1000)+" sec";
    }

    public List<String> getLinks() {
        return links;
    }

    public void setLinks(List<String> links) {
        this.links = links;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
