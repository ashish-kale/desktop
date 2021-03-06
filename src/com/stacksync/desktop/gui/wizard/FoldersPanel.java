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
package com.stacksync.desktop.gui.wizard;

import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.gui.settings.SettingsPanel;
import com.stacksync.desktop.gui.settings.SimpleFolderSelectionPanel;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class FoldersPanel extends SettingsPanel {    
    private SettingsPanel pnlFolders;    
    
    /** Creates new form ProfileBasicsPanel */
    public FoldersPanel(Profile profile) {
        this.profile = profile;
        
        initComponents();
	
        //if (config.isExtendedMode()) {
        //    pnlFolders = new FoldersTablePanel(profile);
        //} else {
        pnlFolders = new SimpleFolderSelectionPanel(profile);
        //}
        
        scrFolders.setViewportView(pnlFolders);
        
        /// setting text ///
        lblTitle.setText(resourceBundle.getString("fpw_what_to_sync"));
        lblIntro1.setText(resourceBundle.getString("fpw_folder_message_part1"));
        lblIntro2.setText(resourceBundle.getString("fpw_folder_message_part2"));                
    }

    @Override
    public void load() {
        pnlFolders.load();
    }

    @Override
    public void save() {
        pnlFolders.save();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblTitle = new javax.swing.JLabel();
        lblIntro1 = new javax.swing.JLabel();
        lblIntro2 = new javax.swing.JLabel();
        scrFolders = new javax.swing.JScrollPane();

        lblTitle.setFont(lblTitle.getFont().deriveFont(lblTitle.getFont().getStyle() | java.awt.Font.BOLD, lblTitle.getFont().getSize()+2));
        lblTitle.setText("__Which folders would you like to sync?");
        lblTitle.setName("lblTitle"); // NOI18N

        lblIntro1.setText("__Stacksync can synchronize any folder on your computer. Please choose");
        lblIntro1.setName("lblIntro1"); // NOI18N

        lblIntro2.setText("__the folders by clicking 'Add Sync Folder'.");
        lblIntro2.setName("lblIntro2"); // NOI18N

        scrFolders.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        scrFolders.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrFolders.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scrFolders.setViewportBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        scrFolders.setName("scrFolders"); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblTitle)
                            .addComponent(lblIntro1)
                            .addComponent(lblIntro2)))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(scrFolders)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(lblTitle)
                .addGap(6, 6, 6)
                .addComponent(lblIntro1)
                .addGap(4, 4, 4)
                .addComponent(lblIntro2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scrFolders, javax.swing.GroupLayout.PREFERRED_SIZE, 293, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(22, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel lblIntro1;
    private javax.swing.JLabel lblIntro2;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JScrollPane scrFolders;
    // End of variables declaration//GEN-END:variables

    @Override
    public void clean() { 
        pnlFolders.clean();
    }
    
    @Override
    public boolean check() {        
        return pnlFolders.check();
    }
    
}