package com.axis.system.jenkins.plugins.axispoolmanager.pipeline.CheckInStep

f = namespace(lib.FormTagLib)
f.entry(field: 'checkInType', title: 'Check in type') {
    f.textbox(value: instance == null ? '' : instance.checkInType)
}
f.entry(field: 'resourceGroupId', title: 'Resource group Id') {
    f.textbox(value: instance == null ? '' : instance.resourceGroupId)
}