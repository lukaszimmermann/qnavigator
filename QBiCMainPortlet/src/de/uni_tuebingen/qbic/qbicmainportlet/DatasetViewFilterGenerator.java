/*******************************************************************************
 * QBiC Project qNavigator enables users to manage their projects.
 * Copyright (C) "2016”  Christopher Mohr, David Wojnar, Andreas Friedrich
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
 *******************************************************************************/
package de.uni_tuebingen.qbic.qbicmainportlet;

import java.io.Serializable;

import org.tepi.filtertable.FilterGenerator;

import com.vaadin.data.Container.Filter;
import com.vaadin.data.util.filter.Compare;
import com.vaadin.data.util.filter.Or;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Field;
import com.vaadin.ui.Notification;

@SuppressWarnings("serial")
public class DatasetViewFilterGenerator implements FilterGenerator, Serializable {


  @Override
  public Filter generateFilter(Object propertyId, Object value) {
    if ("id".equals(propertyId)) {
      /* Create an 'equals' filter for the ID field */
      if (value != null && value instanceof String) {
        try {
          return new Compare.Equal(propertyId, Integer.parseInt((String) value));
        } catch (NumberFormatException ignored) {
          // If no integer was entered, just generate default filter
        }
      }
    } else if ("checked".equals(propertyId)) {
      if (value != null && value instanceof Boolean) {
        if (Boolean.TRUE.equals(value)) {
          return new Compare.Equal(propertyId, value);
        } else {
          return new Or(new Compare.Equal(propertyId, true), new Compare.Equal(propertyId, false));
        }
      }
    }
    // For other properties, use the default filter
    return null;
  }

  @Override
  public Filter generateFilter(Object propertyId, Field<?> originatingField) {
    // Use the default filter
    return null;
  }

  @Override
  public AbstractField<?> getCustomFilterComponent(Object propertyId) {
    // removed custom filter component for id
    if ("checked".equals(propertyId)) {
      CheckBox box = new CheckBox();
      return box;
    }
    return null;
  }

  @Override
  public void filterRemoved(Object propertyId) {
    Notification n =
        new Notification("Filter removed from: " + propertyId, Notification.Type.TRAY_NOTIFICATION);
    n.setDelayMsec(800);
    // n.show(Page.getCurrent());
  }

  @Override
  public void filterAdded(Object propertyId, Class<? extends Filter> filterType, Object value) {
    Notification n =
        new Notification("Filter added to: " + propertyId, Notification.Type.TRAY_NOTIFICATION);
    n.setDelayMsec(800);
    // n.show(Page.getCurrent());
  }

  @Override
  public Filter filterGeneratorFailed(Exception reason, Object propertyId, Object value) {
    /* Return null -> Does not add any filter on failure */
    return null;
  }
}
