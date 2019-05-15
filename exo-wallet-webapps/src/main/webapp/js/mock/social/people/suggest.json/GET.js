module.exports = function (request, response) {
  if (request.query.typeOfRelation) {
    if (request.query.typeOfRelation === 'member_of_space') {
      response.sendFile('GET_from_space.json', {root: __dirname});
    } else if (request.query.typeOfRelation === 'mention_activity_stream') {
      response.sendFile('GET_all_users.json', {root: __dirname});
    } else {
      response.status(404);
    }
  } else {
    response.status(404);
  }
}