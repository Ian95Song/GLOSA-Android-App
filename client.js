const https = require('https');
const fs = require('fs');
const path = require('path');
var outputDir = path.resolve(__dirname, '../dcaitiOutput')

if (!fs.existsSync(outputDir)){
    fs.mkdirSync(outputDir);
}
https.get('https://werkzeug.dcaiti.tu-berlin.de/0432l770/trafficlights/spats?intersection=14052@berlin',{'auth':'username'+':'+'password'},(res) => {
  if (res.statusCode==401)
  console.log("401 unauthorized")
  let movementStates = []
  let spatString = ''
  res.on('data', (d) => {
    try {
        spatString += d.toString()
        spat = JSON.parse(spatString)
        movementStates = spat.intersectionStates[0].movementStates
        if(movementStates.length > 1){
          fs.writeFileSync(path.resolve(outputDir, spat.timestamp.toString() +'_rawData'+'.json'), spatString)
          spatString = ''
          fs.writeFileSync(path.resolve(outputDir, spat.timestamp.toString() +'_MovementStates'+'.json'), JSON.stringify(movementStates))
          console.log("Json file of current signal phase downloaded");
          //just get once json data. If comment next line json data will be got continually
          process.exit(1)
        } else {
          spatString = ''
        }
    }
    catch (error_1) {
        //console.log("uncompleted block");
    }
  });

}).on('error', (e) => {
  console.error(e);
});