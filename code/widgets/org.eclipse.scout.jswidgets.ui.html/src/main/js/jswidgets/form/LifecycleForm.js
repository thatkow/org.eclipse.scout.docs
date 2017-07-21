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
jswidgets.LifecycleForm = function() {
  jswidgets.LifecycleForm.parent.call(this);
  this.data = {};
};
scout.inherits(jswidgets.LifecycleForm, scout.Form);

jswidgets.LifecycleForm.prototype._jsonModel = function() {
  return scout.models.getModel('jswidgets.LifecycleForm');
};

jswidgets.LifecycleForm.prototype._init = function(model) {
  jswidgets.LifecycleForm.parent.prototype._init.call(this, model);

  this.importData(this.data); // FIXME CGU or in open?
};

jswidgets.LifecycleForm.prototype.importData = function(data) {
  this.widget('NameField').setValue(data.name);
  this.widget('BirthdayField').setValue(data.birthday);
};

jswidgets.LifecycleForm.prototype.exportData = function() {
  return {
    name: this.widget('NameField').value,
    birthday: this.widget('BirthdayField').value,
  };
};

jswidgets.LifecycleForm.prototype._save = function() {
  return $.resolvedPromise().then(function() {
    return scout.Status.ok();
  });
};
