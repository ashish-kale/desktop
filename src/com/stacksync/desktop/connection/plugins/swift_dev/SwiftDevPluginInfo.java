/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package com.stacksync.desktop.connection.plugins.swift_dev;

import com.stacksync.desktop.connection.plugins.swift.SwiftConnection;
import java.util.ResourceBundle;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.connection.plugins.Connection;
import com.stacksync.desktop.connection.plugins.PluginInfo;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class SwiftDevPluginInfo extends PluginInfo {
    private final Config config = Config.getInstance();
    
    public static final String ID = "swift_dev";
    private ResourceBundle resourceBundle;

    public SwiftDevPluginInfo() {
         resourceBundle = config.getResourceBundle();
    }        
    
    @Override
    public String getId() {
        return ID;
    }    
    
    @Override
    public String getName() {
        return resourceBundle.getString("swift_plugin_name");
    }

    @Override
    public Integer[] getVersion() {
        return new Integer[] { 0, 1 };
    }

    @Override
    public String getDescripton() {
        return resourceBundle.getString("swift_plugin_description");
    }

    @Override
    public Connection createConnection() {
        return new SwiftConnection();
    }
}
