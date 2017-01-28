'use strict';

const specials = {
  ':':  '_COLON_',
  '+':  '_PLUS_',
  '>':  '_GT_',
  '<':  '_LT_',
  '=':  '_EQ_',
  '~':  '_TILDE_',
  '!':  '_BANG_',
  '@':  '_CIRCA_',
  '#':  '_SHARP_',
  "'":  '_SINGLEQUOTE_',
  '"':  '_DOUBLEQUOTE_',
  '%':  '_PERCENT_',
  '^':  '_CARET_',
  '&':  '_AMPERSAND_',
  '*':  '_STAR_',
  '|':  '_BAR_',
  '{':  '_LBRACE_',
  '}':  '_RBRACE_',
  '[':  '_LBRACK_',
  ']':  '_RBRACK_',
  '/':  '_SLASH_',
  '\\': '_BSLASH_',
  '?':  '_QMARK_',
  '-':  '_',
  '.':  '_'};

module.exports = {
  munge: s => Array.prototype.map.call(s, c => specials[c] || c).join('')
};
