package com.axis.system.jenkins.plugins.axispoolmanager.pipeline.CheckOutStep

f = namespace(lib.FormTagLib)
f.entry(field: 'resourceGroupId', title: 'Resource group Id') {
    f.textbox(value: instance == null ? '' : instance.resourceGroupId)
}
f.entry(field: 'leaseTime', title: 'Lease time') {
    f.textbox(value: instance == null ? '' : instance.leaseTime)
}
f.entry(field: 'resource', title: 'Resource') {
    f.textarea(value: '')
}