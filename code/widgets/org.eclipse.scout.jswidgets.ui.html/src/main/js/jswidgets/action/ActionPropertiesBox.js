/*******************************************************************************
 * Copyright (c) 2017 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
jswidgets.ActionPropertiesBox = function() {
  jswidgets.ActionPropertiesBox.parent.call(this);
  this.field = null;
};
scout.inherits(jswidgets.ActionPropertiesBox, scout.GroupBox);

jswidgets.ActionPropertiesBox.prototype._init = function(model) {
  jswidgets.ActionPropertiesBox.parent.prototype._init.call(this, model);

  this._setField(this.field);
};

jswidgets.ActionPropertiesBox.prototype._jsonModel = function() {
  return scout.models.getModel('jswidgets.ActionPropertiesBox');
};

jswidgets.ActionPropertiesBox.prototype.setField = function(field) {
  this.setProperty('field', field);
};

jswidgets.ActionPropertiesBox.prototype._setField = function(field) {
  this._setProperty('field', field);
  this.setEnabled(this.field);
  if (!this.field) {
    return;
  }
  var enabledField = this.widget('EnabledField');
  enabledField.setValue(this.field.enabled);
  enabledField.on('propertyChange', this._onPropertyChange.bind(this));

  var visibleField = this.widget('VisibleField');
  visibleField.setValue(this.field.visible);
  visibleField.on('propertyChange', this._onPropertyChange.bind(this));

  var textField = this.widget('TextField');
  textField.setValue(this.field.text);
  textField.on('propertyChange', this._onPropertyChange.bind(this));

  var iconIdField = this.widget('IconIdField');
  iconIdField.setValue(this.field.iconId);
  iconIdField.on('propertyChange', this._onPropertyChange.bind(this));

  var tooltipTextField = this.widget('TooltipTextField');
  tooltipTextField.setValue(this.field.tooltipText);
  tooltipTextField.on('propertyChange', this._onPropertyChange.bind(this));

  var horitonalAlignmentField = this.widget('HorizontalAlignmentField');
  horitonalAlignmentField.setValue(this.field.horitonalAlignment);
  horitonalAlignmentField.on('propertyChange', this._onPropertyChange.bind(this));
};

jswidgets.ActionPropertiesBox.prototype._onPropertyChange = function(event) {
  if (event.propertyName === 'value' && event.source.id === 'EnabledField') {
    this.field.setEnabled(event.newValue);
  } else if (event.propertyName === 'value' && event.source.id === 'VisibleField') {
    this.field.setVisible(event.newValue);
  } else if (event.propertyName === 'value' && event.source.id === 'TextField') {
    this.field.setText(event.newValue);
  } else if (event.propertyName === 'value' && event.source.id === 'IconIdField') {
    this.field.setIconId(event.newValue);
  } else if (event.propertyName === 'value' && event.source.id === 'TooltipTextField') {
    this.field.setTooltipText(event.newValue);
  } else if (event.propertyName === 'value' && event.source.id === 'HorizontalAlignmentField') {
    this.field.setHorizontalAlignment(event.newValue);
  }

};
