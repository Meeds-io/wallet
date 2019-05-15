module.exports = function (request, response) {
  if (request.query.name && request.query.type) {
    response.sendFile(`GET_${request.query.type}_${request.query.name}.json`, {root: __dirname});
  } else if (request.query.address) {
    response.sendFile(`GET_${request.query.address.toLowerCase()}.json`, {root: __dirname});
  } else {
    response.status(404);
  }
}