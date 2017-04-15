/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package inr.numass.scripts

import inr.numass.client.NumassClient

NumassClient client = new NumassClient("192.168.111.1", 8335);
print client.startRun("2016_04")