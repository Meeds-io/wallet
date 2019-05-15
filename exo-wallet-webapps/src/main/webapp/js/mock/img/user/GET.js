module.exports = function (request, response) {
  response.sendFile('GET.png', {root: __dirname});
}